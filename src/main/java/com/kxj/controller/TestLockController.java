package com.kxj.controller;

import com.kxj.service.TestLockService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
public class TestLockController {

    @Resource
    private TestLockService testLockService;
    @GetMapping("redisLock")
    public String test(){
        return testLockService.test();
    }

}
