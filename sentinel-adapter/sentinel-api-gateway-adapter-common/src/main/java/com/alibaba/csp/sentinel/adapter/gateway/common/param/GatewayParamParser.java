/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.adapter.gateway.common.param;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleConverter;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowChecker;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleUtil;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Function;
import com.alibaba.csp.sentinel.util.function.Predicate;

/**
 * @author Eric Zhao
 * @since 1.6.0
 */
public class GatewayParamParser<T> {

    private final RequestItemParser<T> requestItemParser;

    public GatewayParamParser(RequestItemParser<T> requestItemParser) {
        AssertUtil.notNull(requestItemParser, "requestItemParser cannot be null");
        this.requestItemParser = requestItemParser;
    }

    /**
     * Parse parameters for given resource from the request entity on condition of the rule predicate.
     *
     * @param resource      valid resource name
     * @param request       valid request
     * @param rulePredicate rule predicate indicating the rules to refer
     * @return the parameter array
     *
     * 网关过滤器中解析出来的请求的参数数组有两种情况：
     * 1. Object[0]: 0个元素的数组
     * 2. Object[该资源有未配置参数的流控规则 ? 该资源配置了参数的流控规则的数量 + 1 : 该资源配置了参数的流控规则的数量]
     * 最后：不管有几个未配置参数的流控规则，最后这些流控规则的 index 是配置了参数的流控规则的数量 + 1
     * -> 对应解析出的参数数组的最后一位索引下标且设置了默认值为 $D
     */
    public Object[] parseParameterFor(String resource, T request, Predicate<GatewayFlowRule> rulePredicate) {
        if (StringUtil.isEmpty(resource) || request == null || rulePredicate == null) {
            return new Object[0];
        }
        Set<GatewayFlowRule> gatewayRules = new HashSet<>();
        Set<Boolean> predSet = new HashSet<>();
        // 是否有未设置参数的规则
        boolean hasNonParamRule = false;
        for (GatewayFlowRule rule : GatewayRuleManager.getRulesForResource(resource)) {
            if (rule.getParamItem() != null) {
                // 当网关流控规则中设置了参数时，将规则添加到 gatewayRules 中
                gatewayRules.add(rule);
                // 规则执行lambda 表达式并将结果添加到 predSet 中
                predSet.add(rulePredicate.test(rule));
            } else {
                // 只要该资源对应的网关流控规则中有一个没有设置参数，则 hasNonParamRule 为 true
                hasNonParamRule = true;
            }
        }
        /**
         * 没有设置参数的网关流控规则（也就是没有针对请求属性进行配置的规则）并且 hasNonParamRule 为 false
         * 说明该资源没有网关流控规则，直接返回 new Object[0] -> 也就是说该 request 没有参数 -> 会传递到 sentinel 的 slot 中
         * -> 这种情况网关流控 slot 是不会去check 的直接返回 true
         * @see ParamFlowChecker#passCheck(ResourceWrapper, ParamFlowRule, int, Object...) ->  if (args.length <= paramIdx) -> return true
         */
        if (!hasNonParamRule && gatewayRules.isEmpty()) {
            return new Object[0];
        }
        /**
         * predSet大于 1 或者 predSet 中包含 false -> set 是不可重复的，如果数量大于1 说明该资源同时拥有 route 维度和 自定义 api 维度的网关流控规则
         * 说明同一资源（资源名称一样）不能同时拥有 route 维度和 自定义 api 维度的网关流控规则
         * 否则直接返回 new Object[0] -> 也就是说该 request 没有参数 -> 会传递到 sentinel 的 slot 中
         * -> 这种情况网关流控 slot 是不会去check 的直接返回 true
         * @see ParamFlowChecker#passCheck(ResourceWrapper, ParamFlowRule, int, Object...) ->  if (args.length <= paramIdx) -> return true
         */
        if (predSet.size() > 1 || predSet.contains(false)) {
            return new Object[0];
        }
        /**
         * 获取网关流控规则中设置的参数索引 {@link GatewayParamFlowItem#index} 的最大值
         * 这里为啥 当网关流控规则中有未设置参数的规则时（hasNonParamRule 为 true）需要将 gatewayRules（设置了参数的规则） 的数量 + 1 ？？？
         * @see com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleConverter#applyNonParamToParamRule(GatewayFlowRule, int) 看下调用这个方法的地方的说明
         * 没有设置参数的网关流控规则（也就是没有针对请求属性进行配置的规则）它的 index 是 idxMap 的 last position (也就是 有参数规则的数量 + 1)
         */
        int size = hasNonParamRule ? gatewayRules.size() + 1 : gatewayRules.size();
        Object[] arr = new Object[size];
        for (GatewayFlowRule rule : gatewayRules) {
            GatewayParamFlowItem paramItem = rule.getParamItem();
            /**
             * 这个 index 是在 {@link com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleConverter#applyToParamRule(GatewayFlowRule, int)} 中设置的（gateway 加载/更新 sentinel 网关流控
             * 规则并在内部将网关流控规则转成热点参数规则的时候）
             * 取的是 GatewayParamFlowItem 中的 index
             */
            int idx = paramItem.getIndex();
            /**
             * 解析请求中的参数是否和网关流控规则中设置的参数匹配
             * 如果网关流控规则没有针对请求属性进行配置 -> 返回 new Object[0] 也就是没有参数
             * 如果网关流控规则针对请求属性进行了配置 -> 如果请求中的参数和规则设置的匹配则返回参数值，否则返回 $NM (也就是 not match)
             * @see GatewayParamParser#parseWithMatchStrategyInternal(int, String, String)
             *
             * $NM -> not match -> 也就是请求中的参数和规则设置的参数不匹配 -> 说明不需要进行流控
             * 在更新流控规则时会构造好 $NM 对应的参数流控配置
             * {@link GatewayRuleManager.GatewayRulePropertyListener#configUpdate(Set)} -> {@link GatewayRuleConverter#generateNonMatchPassParamItem()}
             * -> {@link ParamFlowRuleUtil#buildParamRuleMap(List, Function, Predicate, boolean)}
              */
            String param = parseInternal(paramItem, request);
            /**
             * 将该资源所有的 {@link GatewayParamFlowItem} 中的 index 作为数组的下标，将解析后的request 中的参数值作为数组的值
             * 该 args 数组会传递到 sentinel 的 slot 中 -> {@link com.alibaba.csp.sentinel.adapter.gateway.common.slot.GatewayFlowSlot} 中执行 check 的时候会使用到
             * idx -> {@link GatewayParamFlowItem#index} -> {@link com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule#paramIdx}
              */
            arr[idx] = param;
        }
        if (hasNonParamRule) {
            /**
             * 没有针对请求属性进行配置的网关流控规则 (不管有几个，它的{@link GatewayParamFlowItem#index} 都是一样的 idx array 的 last position)
             * @see com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleConverter#applyNonParamToParamRule(GatewayFlowRule, int)
             * idx array 的最后一个位置设置默认值 $D -> {@link SentinelGatewayConstants#GATEWAY_DEFAULT_PARAM}
             */
            arr[size - 1] = SentinelGatewayConstants.GATEWAY_DEFAULT_PARAM;
        }
        return arr;
    }

    private String parseInternal(GatewayParamFlowItem item, T request) {
        switch (item.getParseStrategy()) {
            case SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP:
                return parseClientIp(item, request);
            case SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HOST:
                return parseHost(item, request);
            case SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER:
                return parseHeader(item, request);
            case SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM:
                return parseUrlParameter(item, request);
            case SentinelGatewayConstants.PARAM_PARSE_STRATEGY_COOKIE:
                return parseCookie(item, request);
            default:
                return null;
        }
    }

    private String parseClientIp(/*@Valid*/ GatewayParamFlowItem item, T request) {
        String clientIp = requestItemParser.getRemoteAddress(request);
        String pattern = item.getPattern();
        if (StringUtil.isEmpty(pattern)) {
            return clientIp;
        }
        return parseWithMatchStrategyInternal(item.getMatchStrategy(), clientIp, pattern);
    }

    private String parseHeader(/*@Valid*/ GatewayParamFlowItem item, T request) {
        String headerKey = item.getFieldName();
        String pattern = item.getPattern();
        // TODO: what if the header has multiple values?
        String headerValue = requestItemParser.getHeader(request, headerKey);
        if (StringUtil.isEmpty(pattern)) {
            return headerValue;
        }
        // Match value according to regex pattern or exact mode.
        return parseWithMatchStrategyInternal(item.getMatchStrategy(), headerValue, pattern);
    }

    private String parseHost(/*@Valid*/ GatewayParamFlowItem item, T request) {
        String pattern = item.getPattern();
        String host = requestItemParser.getHeader(request, "Host");
        if (StringUtil.isEmpty(pattern)) {
            return host;
        }
        // Match value according to regex pattern or exact mode.
        return parseWithMatchStrategyInternal(item.getMatchStrategy(), host, pattern);
    }

    private String parseUrlParameter(/*@Valid*/ GatewayParamFlowItem item, T request) {
        String paramName = item.getFieldName();
        String pattern = item.getPattern();
        String param = requestItemParser.getUrlParam(request, paramName);
        if (StringUtil.isEmpty(pattern)) {
            return param;
        }
        // Match value according to regex pattern or exact mode.
        return parseWithMatchStrategyInternal(item.getMatchStrategy(), param, pattern);
    }

    private String parseCookie(/*@Valid*/ GatewayParamFlowItem item, T request) {
        String cookieName = item.getFieldName();
        String pattern = item.getPattern();
        String param = requestItemParser.getCookieValue(request, cookieName);
        if (StringUtil.isEmpty(pattern)) {
            return param;
        }
        // Match value according to regex pattern or exact mode.
        return parseWithMatchStrategyInternal(item.getMatchStrategy(), param, pattern);
    }

    private String parseWithMatchStrategyInternal(int matchStrategy, String value, String pattern) {
        if (value == null) {
            return null;
        }
        switch (matchStrategy) {
            case SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT:
                return value.equals(pattern) ? value : SentinelGatewayConstants.GATEWAY_NOT_MATCH_PARAM;
            case SentinelGatewayConstants.PARAM_MATCH_STRATEGY_CONTAINS:
                return value.contains(pattern) ? value : SentinelGatewayConstants.GATEWAY_NOT_MATCH_PARAM;
            case SentinelGatewayConstants.PARAM_MATCH_STRATEGY_REGEX:
                Pattern regex = GatewayRegexCache.getRegexPattern(pattern);
                if (regex == null) {
                    return value;
                }
                return regex.matcher(value).matches() ? value : SentinelGatewayConstants.GATEWAY_NOT_MATCH_PARAM;
            default:
                return value;
        }
    }
}
