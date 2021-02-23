package com.rosenberg.retryqueue.core.client;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.rosenberg.retryqueue.core.service.DelayTaskPublish;
import com.rosenberg.retryqueue.domain.RetryTask;
import org.springframework.beans.factory.annotation.Autowired;

public class DelayWorkClient {
    @Autowired
    private DelayTaskPublish taskPublish;

    private Executor executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("retry_task_publish"));

    public void submitDelayTask(RetryTask task) {
        executor.execute(() -> taskPublish.publishDelayTask(task));
    }

}
