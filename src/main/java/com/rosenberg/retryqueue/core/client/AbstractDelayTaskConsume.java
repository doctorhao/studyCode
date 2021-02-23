package com.rosenberg.retryqueue.core.client;

import com.rosenberg.retryqueue.domain.RetryTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.fastjson.JSONObject;

public abstract class AbstractDelayTaskConsume implements IDelayTaskConsume {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean consume(RetryTask task) {
        try {
            return consumeTask(task);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } finally {

        }

        return false;
    }

    /**
         * 校验任务是否已经过期 || 已经被执行过
     *
     * @param task
     * @return
     */
    public boolean taskValid(RetryTask task) {
        Object taskNew = redisTemplate.opsForValue().get(task.getTaskId());
        return task.hasBeenExpired(task.getTaskId(),taskNew)  ||
        	   task.hasBeenExecuted(JSONObject.parseObject(taskNew.toString(), RetryTask.class));
    }


    /**
     * @param
     * @return true: 正常执行   false:出现异常，会进行重试
     */
    public abstract boolean consumeTask(RetryTask task) throws NoSuchFieldException, IllegalAccessException;
}
