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

import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public interface DynamicNacosRuleProvider<T> extends DynamicRuleProvider<T> {
    /**
     * 初始化规则 - 客户端发送心跳包进来注册新的 machine 时进行初始化 rules
     *
     * @param app
     * @return
     * @throws Exception
     */
    void initRules(String app) throws Exception;



}
