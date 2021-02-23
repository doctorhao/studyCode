package com.rosenberg.retryqueue.core.service.redis;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rosenberg.retryqueue.config.RetryTaskAutoConfiguration;
import com.rosenberg.retryqueue.core.service.DelayTaskPublish;
import com.rosenberg.retryqueue.core.service.DelayTaskReceive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.rosenberg.retryqueue.core.util.CustomCollectionUtils;
import com.rosenberg.retryqueue.domain.RetryTask;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisDelayTaskReceive implements DelayTaskReceive {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static long TEN_DAY = 10 * 24 * 60 * 60 * 1000;  //查询过去十天内的任务

    private AtomicBoolean start = new AtomicBoolean(false);

    private Thread messageReceiveThead;

    @Autowired
    private RetryTaskAutoConfiguration retryTaskAutoConfiguration;

    @Autowired
    private DelayTaskPublish delayTaskPublish;

    @Override
    public void receiveDelayTask(BlockingQueue<RetryTask> taskQueue) {
        if (start.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Set<String> taskIdSet = redisTemplate.boundZSetOps(RetryTask.bindTopicKey(retryTaskAutoConfiguration.buildAppName()))
                                    .rangeByScore(System.currentTimeMillis() - TEN_DAY, System.currentTimeMillis());

                            if (CollectionUtils.isEmpty(taskIdSet))
                                TimeUnit.MILLISECONDS.sleep(500);

                            for (String taskId : taskIdSet) {
                                Object taskDetail = redisTemplate.opsForValue().get(taskId);
                                if (hasBeenExpired(taskId, taskDetail)) {
                                    remove(taskId);
                                    continue;
                                }
                                RetryTask delayTask = JSON.parseObject((String) taskDetail, RetryTask.class);
                                if(!taskQueue.contains(delayTask)) taskQueue.put(delayTask);
                            }
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException e) {
                        	log.info("重试任务接收异常 InterruptedException: {}", e);
                        } catch (Exception e) {
                        	log.info("重试任务接收异常 Exception: {}", e);
                        }
                    }
                }
            }, "delay_task_receive").start();

//            this.messageReceiveThead.start();
        }
    }

    private boolean hasBeenExpired(String taskId,Object taskDetail) {
    	boolean hasExpired;
    	if(hasExpired = (taskDetail == null))
    		log.info("任务已经过期或已经执行完所有重试点, taskId : {}", taskId);
    	return hasExpired;
    }

    @Override
    public void ack(RetryTask task) {
        remove(task.getTaskId());
        if(log.isDebugEnabled()) log.debug("删除重试任务 taskId:{}", task.getTaskId());
    }

    /**
         *   删除重试队列任务
     *
     * @param taskId
     */
    private void remove(String taskId) {
        redisTemplate.boundZSetOps(RetryTask.bindTopicKey(retryTaskAutoConfiguration.buildAppName())).remove(taskId);
        redisTemplate.delete(taskId);
    }

    @Override
    public void nack(RetryTask task) {
    	List<Integer> aftRemoveFirstDelayTimes;
        if(beAllRetryTaskExecuted(aftRemoveFirstDelayTimes = CustomCollectionUtils.firstEleRemoveEles(task.getRetryInfo().getRetryStrategy().getRetryTimePoints()))) {
        	remove(task.getTaskId());
        	return;
        }

        //重回队列延迟再处理
        delayTaskPublish.publishDelayTask(buildNewRetryTask(task, aftRemoveFirstDelayTimes));
    }

    /**
        * 构建新的重试任务,去除重试完的时间点。重置执行时间点和过期时间
     *
     * @param task
     */
    private RetryTask buildNewRetryTask(RetryTask task, List<Integer> aftRemoveFirstDelayTimes) {
    	  task.getRetryInfo().getRetryStrategy().setRetryTimePoints(aftRemoveFirstDelayTimes);
          int time = aftRemoveFirstDelayTimes.get(0);
          long executeTime = task.executorTime(time, task.getRetryInfo().getRetryStrategy().getRetryTimeType());
          task.setExecuteTime(executeTime);

          return task;
    }

    /**
         *  配置的所有执行时间点都执行成功
     *
     * @param aftRemoveFirstDelayTimes
     * @return
     */
    private boolean beAllRetryTaskExecuted(List<Integer> aftRemoveFirstDelayTimes) {
    	return CollectionUtils.isEmpty(aftRemoveFirstDelayTimes);
    }
}
