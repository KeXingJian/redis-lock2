package com.kxj.service;

import cn.hutool.core.util.IdUtil;
import com.kxj.factory.LockFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


@Slf4j
@Service
public class TestLockService {

    @Resource
    private LockFactory lockFactory;

    public String test(){
        Lock lock = lockFactory.getLock("redis");

        lock.lock();
        try {
            //执行业务代码
            //测试重入
            entry();

            TimeUnit.MINUTES.sleep(1);


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return Thread.currentThread().getName()+ IdUtil.simpleUUID()+"完成业务";
    }

    private void entry() {
        Lock lock = lockFactory.getLock("redis");

        lock.lock();
        try {
            log.info("重入成功");
        }finally {
            lock.unlock();
        }

    }

}
