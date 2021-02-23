package com.rosenberg.retryqueue.core.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 设置任务上下文
 */
public class TaskContext {

    /**
     * 任务ID上下文
     */
    private static ThreadLocal<String> taskIdThreadLocal = new ThreadLocal<>();


    /**
     * 从上下文中获取任务ID
     *
     * @return
     */
    public static String getTaskId() {
    	String taskId = taskIdThreadLocal.get();

    	if(StringUtils.isNotBlank(taskId)) {
    		return taskId.trim();
    	}

        return taskId;
    }

    /**
     * 设置任务ID的上下文
     *
     * @param companyId
     */
    public static void setTaskId(String taskId) {
    	taskIdThreadLocal.set(taskId);
    }

    /**
     * 移除任务ID
     */
    public static void removeTaskId() {
    	taskIdThreadLocal.remove();
    }
}
