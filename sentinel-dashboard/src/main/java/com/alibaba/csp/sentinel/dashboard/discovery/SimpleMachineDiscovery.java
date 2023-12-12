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
package com.alibaba.csp.sentinel.dashboard.discovery;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.csp.sentinel.dashboard.controller.FlowControllerV1;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.DynamicNacosRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.flow.FlowRuleNacosProvider;
import com.alibaba.csp.sentinel.util.AssertUtil;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author leyou
 */
@Component
public class SimpleMachineDiscovery implements MachineDiscovery {
    private final Logger logger = LoggerFactory.getLogger(SimpleMachineDiscovery.class);
    private final ConcurrentMap<String, AppInfo> apps = new ConcurrentHashMap<>();
    @Autowired
    private List<DynamicNacosRuleProvider> ruleProviders;

    @Override
    public long addMachine(MachineInfo machineInfo) {
        AssertUtil.notNull(machineInfo, "machineInfo cannot be null");
        boolean needInit = !apps.containsKey(machineInfo.getApp());
        AppInfo appInfo = apps.computeIfAbsent(machineInfo.getApp(), o -> new AppInfo(machineInfo.getApp(), machineInfo.getAppType()));
        appInfo.addMachine(machineInfo);
        if (needInit) {
            // 这个needInit 会有并发问题，但是在 reposity 执行 initRules 时会有一个 AtomicBoolean 保证只执行一次避免重复初始化规则
            // 但是就算多次执行初始化也不会有问题，因为最终的规则会被覆盖
            // 从nacos 拉取 rules
            for (DynamicNacosRuleProvider ruleProvider : ruleProviders) {
                try {
                    ruleProvider.initRules(machineInfo.getApp());
                } catch (Exception e) {
                    logger.error("init rules fail when add app " + machineInfo.getApp() + "with " + ruleProvider.getClass().getSimpleName(), e);
                }
            }
        }
        return 1;
    }

    @Override
    public boolean removeMachine(String app, String ip, int port) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        AppInfo appInfo = apps.get(app);
        if (appInfo != null) {
            return appInfo.removeMachine(ip, port);
        }
        return false;
    }

    @Override
    public List<String> getAppNames() {
        return new ArrayList<>(apps.keySet());
    }

    @Override
    public AppInfo getDetailApp(String app) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        return apps.get(app);
    }

    @Override
    public Set<AppInfo> getBriefApps() {
        return new HashSet<>(apps.values());
    }

    @Override
    public void removeApp(String app) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        apps.remove(app);
    }

}
