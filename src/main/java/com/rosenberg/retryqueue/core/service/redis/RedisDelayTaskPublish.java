package com.rosenberg.retryqueue.core.service.redis;

import java.util.concurrent.TimeUnit;

import com.rosenberg.retryqueue.core.service.DelayTaskPublish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.rosenberg.retryqueue.domain.RetryTask;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisDelayTaskPublish implements DelayTaskPublish {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private final int DEFAULT_RETRY_ADD_COUNT = 3;

    @Override
    public void publishDelayTask(RetryTask task) {
    	log.info("发布重试任务: {}", task.toJson());
        boolean success = false;
        int retry = 0;
        try {
            while (!success && retry++ < DEFAULT_RETRY_ADD_COUNT) {
                try {
                    redisTemplate.opsForValue().set(task.getTaskId(), task.toJson(), task.expire(), TimeUnit.MILLISECONDS);
                    success = redisTemplate.boundZSetOps(RetryTask.bindTopicKey(task.getRetryInfo().getAppName())).add(task.getTaskId(), task.getExecuteTime());
                } catch (Exception e) {
                    log.error("重试任务:{},发布失败:{}.",task.toJson(),e);
                }
                if(!success) TimeUnit.MICROSECONDS.sleep(100);
            }

        } catch (Exception e) {
            log.error("重试任务: {},发布异常:{}.", task.toJson(), e);
        }
    }
}
