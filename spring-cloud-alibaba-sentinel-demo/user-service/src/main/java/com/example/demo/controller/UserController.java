package com.example.demo.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreaker;
import com.alibaba.csp.sentinel.util.function.BiConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName:UserController
 * Package:com.example.demo.controller
 * Description:
 *
 * @Date:2022/7/29 9:06
 * @Author:qs@1.com
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @RequestMapping("/info/{id}")
    @SentinelResource(value = "userInfo", blockHandler = "handlerBlockException", fallback = "fallback")
    public String info(@PathVariable Integer id) {
        return "the user identity is --------> " + id ;
    }

    public String handlerBlockException(Integer id, BlockException ex) {
        Context context = ContextUtil.getContext();
        Entry entry = context.getCurEntry();
        entry.whenTerminate(new BiConsumer<Context, Entry>() {
            @Override
            public void accept(Context context, Entry entry) {
                System.out.println(
                        String.format("execute finally method ---> exit：curNode ===> %s, lastNode ===> %s , originNode ===> %s",
                                entry.getCurNode(), entry.getLastNode(), entry.getOriginNode()));
            }
        });
        return "======> flow conttoller -> handler blockexception" ;
    }

    public String fallback(Integer id, Throwable ex) {
        return "========> fallback handler" ;
    }

    // SentinelAutoCofiguration already register this bean
//    @Bean
//    public SentinelResourceAspect sentinelResourceAspect() {
//        return new SentinelResourceAspect();
//    }

}
