package com.rosenberg.retryqueue.core.client;

import com.rosenberg.retryqueue.domain.RetryTask;

public interface IDelayTaskConsume {
    boolean consume(RetryTask task);
}