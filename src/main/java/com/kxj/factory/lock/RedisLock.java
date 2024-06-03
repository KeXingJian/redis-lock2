package com.kxj.factory.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;


@Slf4j
public class RedisLock implements Lock {

    private final StringRedisTemplate stringRedisTemplate;
    private final String lockName;
    private final String uuid;
    private final Long expireTime=30L;

    public RedisLock(StringRedisTemplate stringRedisTemplate, String lockName,String uuid) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName=lockName;
        this.uuid = Thread.currentThread().getId()+":"+uuid;
    }

    @Override
    public void lock() {
        tryLock();
    }


    @Override
    public void lockInterruptibly() throws InterruptedException {

    }


    @Override
    public boolean tryLock() {
        try {
            tryLock(-1,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {

        if (time==-1){

            //lua脚本若抢到或可重入,未对应锁加1
            String script=
                            "if redis.call('exists',KEYS[1])==0 or redis.call('hexists',KEYS[1],ARGV[1])==1 then " +
                                    "redis.call('hincrby',KEYS[1],ARGV[1],1) " +
                                    "redis.call('expire',KEYS[1],ARGV[2]) return 1 " +
                            "else " +
                                    "return 0 " +
                            "end";

            //未抢到锁,线程等待
            while (Boolean.FALSE.
                    equals(stringRedisTemplate.execute
                            (new DefaultRedisScript<>(script, Boolean.class),
                                    Collections.singletonList(lockName),
                                    uuid,
                                    String.valueOf(expireTime)))){
                log.info(uuid+"抢锁失败持续抢占中");
                //防止刷屏设置3秒,合适时间应为60毫秒
                TimeUnit.MILLISECONDS.sleep(3000);
            }
            log.info(lockName+"进入锁"+" uuid="+uuid);

            //看门狗动态延时
            watchDog();
            return true;
        }
        return false;
    }

    private void watchDog() {

        String script=
                        "if redis.call('HEXISTS',KEYS[1],ARGV[1])==1 " +
                            "then return redis.call('expire',KEYS[1],ARGV[2]) " +
                        "else " +
                            "return 0 " +
                        "end";
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (Boolean.TRUE.equals(stringRedisTemplate.execute
                        (new DefaultRedisScript<>(script, Boolean.class),
                                Collections.singletonList(lockName),
                                uuid,
                                String.valueOf(expireTime)))) {
                    watchDog();
                    log.info("延时成功成功");
                }
            }
        },1000*(expireTime/3));
    }


    @Override
    public void unlock() {
        //不存在锁则程序错误,
        String script=
                        "if redis.call('HEXISTS',KEYS[1],ARGV[1])==0 then " +
                            "return nil " +
                        "elseif " +
                            "redis.call('HINCRBY',KEYS[1],ARGV[1],-1)==0 then " +
                            "return redis.call('del',KEYS[1]) " +
                        "else " +
                            "return 0 " +
                        "end";
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(lockName),
                uuid);
        if (result==null){
            log.error("解锁时重大错误,需要紧急修复"+result+" 锁信息为 "+lockName+"进入锁"+" uuid="+uuid);
        }else {
            log.info(lockName+"释放锁"+" uuid="+uuid);
        }
    }


    @Override
    public Condition newCondition() {
        return null;
    }
}
