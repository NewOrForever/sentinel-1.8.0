/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.adapter.gateway.sc;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.ResourceTypeConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.param.GatewayParamParser;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.reactor.ContextConfig;
import com.alibaba.csp.sentinel.adapter.reactor.EntryConfig;
import com.alibaba.csp.sentinel.adapter.reactor.SentinelReactorTransformer;
import com.alibaba.csp.sentinel.adapter.gateway.sc.api.GatewayApiMatcherManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.api.matcher.WebExchangeApiMatcher;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Eric Zhao
 * @since 1.6.0
 */
public class SentinelGatewayFilter implements GatewayFilter, GlobalFilter, Ordered {

    private final int order;

    public SentinelGatewayFilter() {
        this(Ordered.HIGHEST_PRECEDENCE);
    }

    public SentinelGatewayFilter(int order) {
        this.order = order;
    }

    private final GatewayParamParser<ServerWebExchange> paramParser = new GatewayParamParser<>(
        new ServerWebExchangeItemParser());

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        Mono<Void> asyncResult = chain.filter(exchange);
        if (route != null) {
            /**
             * route 维度，微服务使用gateway 网关的情况下，routeId 为微服务的服务名
             * 微服务使用gateway 的模式下该 route 必然是存在的，因为gateway 会根据微服务的服务名创建 route
             * 所以使用 gateway 时 route 维度 entry 必然存在 -> route 维度必然会去走一遍 sentinel 的 slot chain
             * @see com.alibaba.csp.sentinel.adapter.gateway.common.slot.GatewayFlowSlot#checkGatewayParamFlow
             * @see GatewayRuleManager#getConvertedParamRules(String) 获取 routeId 对应的网关流控规则 ->
             * 如果 route 维度的资源（route id）没有设置对应的流控规则（GatewayFlowRule 内部会转成 ParamFlowRule）、降级规则、系统规则 -> 所有的 slot check 都是直接不需要执行的
              */
            String routeId = route.getId();
            Object[] params = paramParser.parseParameterFor(routeId, exchange,
                r -> r.getResourceMode() == SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
            String origin = Optional.ofNullable(GatewayCallbackManager.getRequestOriginParser())
                .map(f -> f.apply(exchange))
                .orElse("");
            asyncResult = asyncResult.transform(
                new SentinelReactorTransformer<>(new EntryConfig(routeId, ResourceTypeConstants.COMMON_API_GATEWAY,
                    EntryType.IN, 1, params, new ContextConfig(contextName(routeId), origin)))
            );
        }

        /**
         * 自定义 API 维度 -> 需要先去 Api 管理菜单中添加 Api 分组 -> 该 Api 分组名称就是自定义 API 维度的资源名称
         * 这里要先获取到匹配的 Api 分组名称 -> 遍历所有的 Api 分组名称解析匹配的参数 -> 每个匹配的 api 分组都会创建一个 自定义API 维度的 entry
         * -> 每个 APi 维度的 entry 也都会走一遍 sentinel 的 slot chain
         * 总结：
         * 1. 如果该请求有匹配的自定义 API 维度的资源，那么该请求会走两遍 sentinel 的 slot chain（route 维度 和 自定义 API 维度）
         * 2. route 维度没校验通过直接抛异常，不会走自定义 API 维度的 slot chain -> route 维度校验通过则继续走自定义 API 维度的 slot chain
         * 3. 如果设置了 2 个 自定义 API 维度的资源，且这两个资源都匹配了，那么该请求会走 3 次 sentinel 的 slot chain（route 维度 和 2个自定义 API 维度）
         */
        // 遍历自定义的 API 分组 ApiDefinition -> 获取与该请求匹配的 ApiDefinition 的名称
        Set<String> matchingApis = pickMatchingApiDefinitions(exchange);
        for (String apiName : matchingApis) {
            // 解析请求中的参数是否和网关流控规则中设置的参数匹配 -> 如果匹配则返回参数值，否则返回 $NM (也就是 not match)
            Object[] params = paramParser.parseParameterFor(apiName, exchange,
                r -> r.getResourceMode() == SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME);
            // 每个匹配的 自定义 API 维度的资源都会创建一个 entry -> 也会走一遍 sentinel 的 slot chain
            asyncResult = asyncResult.transform(
                new SentinelReactorTransformer<>(new EntryConfig(apiName, ResourceTypeConstants.COMMON_API_GATEWAY,
                    EntryType.IN, 1, params))
            );
        }

        return asyncResult;
    }

    private String contextName(String route) {
        return SentinelGatewayConstants.GATEWAY_CONTEXT_ROUTE_PREFIX + route;
    }

    Set<String> pickMatchingApiDefinitions(ServerWebExchange exchange) {
        return GatewayApiMatcherManager.getApiMatcherMap().values()
            .stream()
            .filter(m -> m.test(exchange))
            .map(WebExchangeApiMatcher::getApiName)
            .collect(Collectors.toSet());
    }

    @Override
    public int getOrder() {
        return order;
    }
}
