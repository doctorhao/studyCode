package com.rosenberg.retryqueue.core.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rosenberg.retryqueue.core.service.DelayTaskReceive;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.rosenberg.retryqueue.core.client.AbstractDelayTaskConsume;
import com.rosenberg.retryqueue.core.client.IDelayTaskConsume;
import com.rosenberg.retryqueue.core.client.NamedThreadFactory;
import com.rosenberg.retryqueue.core.service.DistributedLockHandler;
import com.rosenberg.retryqueue.core.util.SpringContext;
import com.rosenberg.retryqueue.domain.RetryTask;
import com.rosenberg.retryqueue.exception.RetryException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelayWorkServer implements InitializingBean {
    private BlockingQueue<RetryTask> taskQueue;

    private Executor consumePool;

    private Thread dispatchThread;

    @Autowired
    private DistributedLockHandler distributedLockHandler;

    @Autowired
    private DelayTaskReceive delayTaskReceive;

    private AtomicBoolean start = new AtomicBoolean(false);

    private void executeRetryTask(RetryTask task) {
        consumePool.execute(() -> {
            String lockKey;
            if (distributedLockHandler.tryLock(lockKey=task.lockKey())) {
                try {
                    log.info("任务获取锁：{}, 任务详情：{}.", lockKey, task.toJson());
                    String beanName;
                    Object bean = SpringContext.getBean(beanName = task.getRetryInfo().getConsumeBeanName());

                    if ((bean == null || !(bean instanceof IDelayTaskConsume))) {
                        log.error("根据BeanName:{},找不到重试任务执行Bean.请检查是否配置了spring.application.name属性.任务详情:{}",beanName,task.toJson());
                        return;
                    }

                    AbstractDelayTaskConsume consumeService = (AbstractDelayTaskConsume) bean;
                    if (consumeService.taskValid(task)) return;
                    if (!task.needRetry()) {
                    	log.error("重试时间点为空,请检查FailRetry注解的重试时间点retryIntervalTimes是否配置正确: {}", task.toJson());
                        delayTaskReceive.ack(task);
                        return;
                    }

                    if (consumeService.consume(task)) {
                    	log.info("重试任务: {},执行成功.", task.toJson());
                        delayTaskReceive.ack(task);
                    } else {
                        throw new RetryException();
                    }
                } catch (RetryException e) {
                    delayTaskReceive.nack(task);
                } catch (Exception e) {
                	log.error("重试任务: {},执行异常：{}.", task.toJson(),e);
                    delayTaskReceive.ack(task);
                } finally {
                    distributedLockHandler.releaseLock(lockKey);
                }
            }
        });
    }

    /**
         * 重试队列调度,生产消费模型，将到达执行时间点的任务放置到阻塞队列中。重试消费者取出任务执行重试
     */
    private void start() {
        if (start.compareAndSet(false, true)) {
            this.taskQueue = new ArrayBlockingQueue<>(100);
            this.consumePool = Executors.newFixedThreadPool(5, new NamedThreadFactory("retry_task_consume"));
            this.dispatchThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            delayTaskReceive.receiveDelayTask(taskQueue);  //获取任务 扔进队列
                            RetryTask task = taskQueue.take();
                            executeRetryTask(task);
                        } catch (InterruptedException e) {
                        	log.error("重试调度任务执行异常：{}.",e);
                        }
                    }
                }
            }, "retry_task_dispatch");

            dispatchThread.start();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }
}
