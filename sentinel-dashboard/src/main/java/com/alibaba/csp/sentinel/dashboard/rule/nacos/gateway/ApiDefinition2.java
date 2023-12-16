package com.alibaba.csp.sentinel.dashboard.rule.nacos.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Fox
 */
public class ApiDefinition2 {
    private String apiName;
    private Set<ApiPathPredicateItem> predicateItems;

    public ApiDefinition2() {
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Set<ApiPathPredicateItem> getPredicateItems() {
        return predicateItems;
    }

    public void setPredicateItems(Set<ApiPathPredicateItem> predicateItems) {
        this.predicateItems = predicateItems;
    }

    @Override
    public String toString() {
        return "ApiDefinition2{" + "apiName='" + apiName + '\'' + ", predicateItems=" + predicateItems + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ApiDefinition2 that = (ApiDefinition2)o;

        if (!Objects.equals(apiName, that.apiName)) { return false; }
        return Objects.equals(predicateItems, that.predicateItems);
    }

    @Override
    public int hashCode() {
        int result = apiName != null ? apiName.hashCode() : 0;
        result = 31 * result + (predicateItems != null ? predicateItems.hashCode() : 0);
        return result;
    }


    public ApiDefinition toApiDefinition() {
        ApiDefinition apiDefinition = new ApiDefinition();
        apiDefinition.setApiName(apiName);

        Set<ApiPredicateItem> apiPredicateItems = new LinkedHashSet<>();
        apiDefinition.setPredicateItems(apiPredicateItems);

        if (predicateItems != null) {
            apiPredicateItems.addAll(predicateItems);
        }

        return apiDefinition;
    }

}
