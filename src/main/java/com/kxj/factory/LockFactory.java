package com.kxj.factory;

import cn.hutool.core.util.IdUtil;
import com.kxj.factory.lock.RedisLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.locks.Lock;
@Component
public class LockFactory {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final String uuid= IdUtil.simpleUUID();

    public Lock getLock(String lockType){
        String lackName="";
        if (lockType.equalsIgnoreCase("redis")){
            lackName="kxjRedis";
            return new RedisLock(stringRedisTemplate,lackName,uuid);
        }else if (lockType.equalsIgnoreCase("mysql")){
            //TODO
            return null;
        }else if (lockType.equalsIgnoreCase("zookeeper")){
            //TODO
            return null;
        }else {
            return null;
        }
    }


}
