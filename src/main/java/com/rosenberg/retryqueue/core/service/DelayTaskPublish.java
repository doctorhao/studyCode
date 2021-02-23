package com.rosenberg.retryqueue.core.service;

import com.rosenberg.retryqueue.domain.RetryTask;

public interface DelayTaskPublish {
    void publishDelayTask(RetryTask task);
}
