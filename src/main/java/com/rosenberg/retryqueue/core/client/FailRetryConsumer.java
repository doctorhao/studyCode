package com.rosenberg.retryqueue.core.client;

import java.lang.reflect.Method;
import java.util.List;

import com.rosenberg.retryqueue.core.util.CustomReflectionUtils;
import com.rosenberg.retryqueue.core.util.SpringContext;
import com.rosenberg.retryqueue.core.util.TaskContext;
import com.rosenberg.retryqueue.domain.RetryInfo;
import com.rosenberg.retryqueue.domain.RetryTask;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.alibaba.fastjson.JSON;

@Component
public class FailRetryConsumer extends AbstractDelayTaskConsume  {
    @Override
    public boolean consumeTask(RetryTask task) throws NoSuchFieldException, IllegalAccessException {
    	Class<?>[] classTypes = null;
        Method method = ReflectionUtils.findMethod(SpringContext.getBean(task.getRetryInfo().getRetryBeanName()).getClass(),
        		                                   task.getRetryInfo().getRetryMethodName(),
        		                                   classTypes = getRequestClasses(task.getRetryInfo().getTargetRequestInfos()));

        //设置任务上下文
        TaskContext.setTaskId(task.getTaskId());

        Class<?> returnType = task.getRetryInfo().getTargetReponseInfo().getReponseClass();
        List<String> errorCodes = task.getRetryInfo().getRetryStrategy().getRetryErrorCodes();
        String errorCodeFiled = task.getRetryInfo().getRetryStrategy().getRetryErrorCodeFiled();

        Object result = ReflectionUtils.invokeMethod(method, SpringContext.getBean(task.getRetryInfo().getRetryBeanName()), getRequestParams(task.getRetryInfo().getTargetRequestInfos(), classTypes));

        if(errorCodes.contains(CustomReflectionUtils.getValue(returnType, errorCodeFiled, result))) {
            return false;
        }

        return true;
    }

    /**
     * 获取入参的class
     *
     * @param requestInfos
     * @return
     */
    private Class<?>[] getRequestClasses(List<RetryInfo.RequestInfo> requestInfos) {
    	Class<?>[] argsClass = new Class<?>[requestInfos.size()];
    	for (int i = 0; i < requestInfos.size(); i++) {
            argsClass[i] = requestInfos.get(i).getRequestClass();
        }

    	return argsClass;
    }

    /**
     * 获取入参的值
     *
     * @param requestInfos
     * @return
     */
    private Object[] getRequestParams(List<RetryInfo.RequestInfo> requestInfos, Class<?>[] classType) {
    	Object[] args = new Object[requestInfos.size()];
    	for (int i = 0; i < requestInfos.size(); i++) {
    		args[i] = JSON.parseObject(JSON.toJSONString(requestInfos.get(i).getParameter()), classType[i]);
        }

    	return args;
    }
 }
