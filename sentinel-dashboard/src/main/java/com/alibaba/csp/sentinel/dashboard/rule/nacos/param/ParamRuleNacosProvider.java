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
package com.alibaba.csp.sentinel.dashboard.rule.nacos.param;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.DynamicNacosRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.NacosConfigUtil;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
@Component("paramRuleNacosProvider")
public class ParamRuleNacosProvider implements DynamicNacosRuleProvider<List<ParamFlowRuleEntity>> {

    @Autowired
    private ConfigService configService;
//    @Autowired
//    private Converter<String, List<ParamFlowRuleEntity>> converter;

    @Autowired
    private InMemoryRuleRepositoryAdapter<ParamFlowRuleEntity> repository;
    @Autowired
    @Qualifier("paramRuleNacosProvider")
    private DynamicRuleProvider<List<ParamFlowRuleEntity>> ruleProvider;

    @Override
    public List<ParamFlowRuleEntity> getRules(String appName, String ip, Integer port) throws Exception {
        String rules = configService.getConfig(appName + NacosConfigUtil.PARAM_FLOW_DATA_ID_POSTFIX,
            NacosConfigUtil.GROUP_ID, 3000);
        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        List<ParamFlowRule> list = JSON.parseArray(rules, ParamFlowRule.class);
        return list.stream().map(rule ->
                        ParamFlowRuleEntity.fromParamFlowRule(appName, ip, port, rule))
                .collect(Collectors.toList());
    }

    @Override
    public void initRules(String app) throws Exception {
        List<ParamFlowRuleEntity> rules = ruleProvider.getRules(app);

        repository.initRules(rules, app);
    }
}
