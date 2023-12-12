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
package com.alibaba.csp.sentinel.dashboard.rule;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public interface DynamicRuleProvider<T> {

    default T getRules(String appName) throws Exception {
        return null;
    }

    default T getRules(String appName, String ip, Integer port) throws Exception {
        return null;
    }


    /**
     * TODO 加载规则并更新内存和 ids 后续在优化吧，避免直接修改 nacos 配置数据后 ids 落后的问题
     * 不不不，这个不能弄，不应该在 nacos 中直接修改 sentinel 的规则配置，而是应该在 sentinel-dashboard 中修改避免不必要的错误数据
     * Load rules from the data source.
     * getRules -> saveAll ->  initIds
     * @param appName
     * @return
     * @throws Exception
     */
//    T loadRules(String appName) throws Exception;
}
