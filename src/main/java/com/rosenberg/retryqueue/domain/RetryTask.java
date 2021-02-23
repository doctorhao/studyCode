package com.rosenberg.retryqueue.domain;

import java.util.Objects;
import java.util.UUID;
import org.springframework.util.CollectionUtils;
import com.alibaba.fastjson.JSON;
import com.rosenberg.retryqueue.core.aop.TimeType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RetryTask {
    private String taskId;  //任务ID
    protected long executeTime; //任务执行时间点
    public final static String TOPIC = "DELAY_QUEUE_T3_";  //任务所在队列
    private RetryInfo retryInfo;  //任务信息

    public RetryTask() {}

    public RetryTask(RetryInfo retryInfo) {
        this.taskId = UUID.randomUUID().toString().replaceAll("-", "");
        this.retryInfo = retryInfo;
        this.executeTime = executorTime(retryInfo.getRetryStrategy().getRetryTimePoints().get(0),
        								retryInfo.getRetryStrategy().getRetryTimeType());
    }

    public long executorTime(int time, TimeType timeType) {
    	long timeUnit;
    	if(timeType.equals(TimeType.HOUR)) {
    		timeUnit = 24 * 60 * 1000;
    	} else if(timeType.equals(TimeType.SECOND)) {
    		timeUnit = 1 * 1000;
    	} else {
    		timeUnit = 60 * 1000;  //默认分钟
    	}

        return System.currentTimeMillis() + time * timeUnit;
    }

    public static String bindTopicKey(String appName) {
    	return TOPIC+appName;
    }


    /**
         *  是否还需重试，重试时间点为空：表示配置的重试时间点已经执行完毕或者未配置重试时间。不需要再进行重试
     *
     * @return
     */
    public boolean needRetry() {
        return !CollectionUtils.isEmpty(this.retryInfo.getRetryStrategy().getRetryTimePoints());
    }

    public String toJson() {
       return JSON.toJSONString(this);
    }

    /**
         *  过期时间,目前设置为一天， 实例挂死，只需在一天内重启仍可重试
     *
     * @return
     */
    public long expire() {
        long expire = this.executeTime - System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        return expire < 0 ? 1000 : expire;
    }

    public String lockKey() {
        return taskId + String.valueOf(this.executeTime).substring(5);
    }

    /**
        * 任务是否已经过期
     *
     * @param taskNew
     * @return
     */
    public boolean hasBeenExpired(String taskId,Object taskNew) {
    	boolean valid;
    	if(valid = (taskNew == null)) log.info("当前任务已经过期，任务ID:{}.", taskId);
    	return valid;
    }

    /**
        * 任务是否已经被执行过
     *
     * @param delayTaskNew
     * @return
     */
    public boolean hasBeenExecuted(RetryTask delayTaskNew) {
        boolean valid = delayTaskNew.getExecuteTime() < this.getExecuteTime();
        if(valid) log.info("当前任务已经被重试执行过,任务新执行点:{}, 任务旧执行点：{}.", delayTaskNew.getExecuteTime(), this.executeTime);
        return valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetryTask retryTask = (RetryTask) o;
        return Objects.equals(taskId, retryTask.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.taskId + this.executeTime);
    }
}

