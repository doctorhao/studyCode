package com.rosenberg.retryqueue.core.service;

import java.util.concurrent.BlockingQueue;

import com.rosenberg.retryqueue.domain.RetryTask;

public interface DelayTaskReceive {
    void receiveDelayTask(BlockingQueue<RetryTask> taskQueue);
    void ack(RetryTask task);
    void nack(RetryTask task);
}
