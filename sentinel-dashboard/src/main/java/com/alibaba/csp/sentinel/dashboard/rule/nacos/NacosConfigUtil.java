/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public final class NacosConfigUtil {

    public static final String GROUP_ID = "SENTINEL_GROUP";

    /**
     * 这些后缀是发布到 nacos 的配置文件的后缀，客户端使用 nacos 的配置中心时，需要在配置文件中指定这些后缀
     * spring:
     *   cloud:
     *     # sentinel 控制台
     *     # @see https://github.com/alibaba/Sentinel/wiki/%E5%90%AF%E5%8A%A8%E9%85%8D%E7%BD%AE%E9%A1%B9
     *     sentinel:
     *       web-context-unify: false
     *       transport:
     *         # sentinel控制台地址
     *         dashboard: localhost:9000
     *         # 指定应用与Sentinel控制台交互的端口，应用本地会起一个该端口占用的HttpServer
     *         # 默认是8719 占用会自动 +1
     *         #        port: 8719
     *       datasource:
     *         flow-rules:
     *           nacos:
     *             server-addr: nacosIp:8848
     *             dataId: ${spring.application.name}-flow-rules
     *             groupId: SENTINEL_GROUP #注意groupId对应SentinelDashboard中的定义
     *             data-type: json
     *             rule-type: flow
     *         degrade-rules:
     *           nacos:
     *             server-addr: 123.60.150.23:8848
     *             dataId: ${spring.application.name}-degrade-rules
     *             groupId: SENTINEL_GROUP
     *             data-type: json
     *             rule-type: degrade
     */
    public static final String FLOW_DATA_ID_POSTFIX = "-flow-rules";
    public static final String DEGRADE_DATA_ID_POSTFIX = "-degrade-rules";
    public static final String AUTHORITY_DATA_ID_POSTFIX = "-authority-rules";
    public static final String PARAM_FLOW_DATA_ID_POSTFIX = "-param-flow-rules";
    public static final String SYSTEM_FLOW_DATA_ID_POSTFIX = "-system-rules";
    public static final String GATEWAY_FLOW_DATA_ID_POSTFIX = "-gateway-flow-rules";
    public static final String GATEWAY_API_DATA_ID_POSTFIX = "-gateway-api-rules";
    public static final String CLUSTER_MAP_DATA_ID_POSTFIX = "-cluster-map";

    /**
     * cc for `cluster-client`
     */
    public static final String CLIENT_CONFIG_DATA_ID_POSTFIX = "-cc-config";
    /**
     * cs for `cluster-server`
     */
    public static final String SERVER_TRANSPORT_CONFIG_DATA_ID_POSTFIX = "-cs-transport-config";
    public static final String SERVER_FLOW_CONFIG_DATA_ID_POSTFIX = "-cs-flow-config";
    public static final String SERVER_NAMESPACE_SET_DATA_ID_POSTFIX = "-cs-namespace-set";

    private NacosConfigUtil() {}

    /**
     * RuleEntity----->Rule
     * @param entities
     * @return
     */
    public static String convertToRule(List<? extends RuleEntity> entities){
        return JSON.toJSONString(
                entities.stream().map(RuleEntity::toRule)
                        .collect(Collectors.toList()));
    }

    /**
     * ApiDefinitionEntity----->ApiDefinition
     * @param entities
     * @return
     */
    public static String convertToApiDefinition(List<? extends ApiDefinitionEntity> entities){
        return JSON.toJSONString(
                entities.stream().map(r -> r.toApiDefinition())
                        .collect(Collectors.toList()));
    }

    /**
     * GatewayFlowRuleEntity----->GatewayFlowRule
     * @param entities
     * @return
     */
    public static String convertToGatewayFlowRule(List<? extends GatewayFlowRuleEntity> entities){
        return JSON.toJSONString(
                entities.stream().map(r -> r.toGatewayFlowRule())
                        .collect(Collectors.toList()));
    }

}
