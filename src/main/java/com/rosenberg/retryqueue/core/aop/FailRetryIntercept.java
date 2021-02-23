package com.rosenberg.retryqueue.core.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rosenberg.retryqueue.config.RetryTaskAutoConfiguration;
import com.rosenberg.retryqueue.domain.RetryInfo;
import com.rosenberg.retryqueue.domain.RetryTask;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import com.alibaba.fastjson.JSON;
import com.rosenberg.retryqueue.core.client.DelayWorkClient;
import com.rosenberg.retryqueue.core.util.CustomBeanUtils;
import com.rosenberg.retryqueue.core.util.CustomCollectionUtils;
import com.rosenberg.retryqueue.core.util.CustomReflectionUtils;
import com.rosenberg.retryqueue.core.util.TaskContext;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Order(Integer.MAX_VALUE)
@Slf4j
public class FailRetryIntercept {
    @Autowired
    private DelayWorkClient delayWorkClient;

    @Autowired
    private RetryTaskAutoConfiguration retryTaskAutoConfiguration;

    @Pointcut("@annotation(com.rosenberg.retryqueue.core.aop.FailRetry)")
    public void doFailRetry() {}

    /**
     * 拦截FailRetry注解  执行环绕方法
     *
     * @param pjp
     * @return JsonResult（被拦截方法的执行结果）
     */
    @Around("doFailRetry()")
    public Object Interceptor(ProceedingJoinPoint pjp) throws Throwable {
    	//1 获取注解信息
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        FailRetry methodAnnotation = method.getAnnotation(FailRetry.class);          //获取方法注解 FailRetry
        boolean startFlag = methodAnnotation.isStart();  //获取是否需要启动重试
        Object[] args = pjp.getArgs();

        //2 注解不启用 || 反射重试调用两种情况， 直接返回不需要再次放置到缓存中
        if(!startFlag || beReflectionInvoke(TaskContext.getTaskId())) {
        	TaskContext.removeTaskId();  //删除任务上下文
            return pjp.proceed(args);
        }

        Object result = null;
        log.warn("类:{}-方法:{}-入参:{}",getClassName(method),method,JSON.toJSONString(args));
        try {
        	result = pjp.proceed(args);

            //3 判断是否需要重试
            if(beNeedRetry(methodAnnotation,method, result)) {
            	 buildAndSubmitRetryTask(method, args);  //4 创建并提交重试任务
            }
        } catch (IllegalArgumentException e) {
            log.error("类:{}-方法:{}-入参:{},拦截失败,参数非法:",getClassName(method),method.getName(),JSON.toJSONString(args),e);
        } catch (Throwable throwable) {
        	log.error("类:{}-方法:{}-入参:{},拦截失败:",getClassName(method),method.getName(),JSON.toJSONString(args),throwable);
        } finally {

        }

        return result;
    }

    /**
     * 创建并提交重试任务
     *
     * @param method
     * @param args
     */
    @SuppressWarnings("unchecked")
	private void buildAndSubmitRetryTask(Method method, Object[] args) {
        delayWorkClient.submitDelayTask(new RetryTask(RetryInfo.builder().appName(retryTaskAutoConfiguration.buildAppName())
        		                                           .retryBeanName(CustomBeanUtils.getBeanNameFromClassName(getClassName(method)))
        		                                           .retryMethodName(method.getName())
														   .targetRequestInfos(buildRetryRequestInfos(method, args))
														   .targetResponseInfo(buildRetryResponseInfo(method.getReturnType()))
														   .retryStrategy(buildRetryStrategy(method.getAnnotation(FailRetry.class))).build()));
    }

    /**
     * 获取目标类名称
     *
     * @param method
     * @return
     */
    private String getClassName(Method method) {
    	 Class<?> targetClass = method.getDeclaringClass();

         //获取方法的名称和class的名称
         return targetClass.getName();
    }

    /**
     * 判断是否需要重试  ，配置需要重试的错误码和执行返回的错误码相同时则需要重试
     *
     * @param errorCode
     * @param returnType
     * @param errorCodeFiled
     * @param result
     * @return
     */
    private boolean beNeedRetry(FailRetry methodAnnotation, Method method , Object result) {
    	String[] retryTimePoints = methodAnnotation.retryIntervalTimes();
    	String[] errorCodes = methodAnnotation.errorCode();
    	String errorCodeFiled = methodAnnotation.errorCodeFiled();
    	if(retryTimePoints == null || retryTimePoints.length == 0) {
    		log.info("请检查类:{}-方法:{}的重试注解是否配置了重试时间点.",getClassName(method),method.getName());
    		return false;
    	}

    	if(errorCodes == null || errorCodes.length == 0) {
    		log.info("请检查类:{}-方法:{}的重试注解是否配置了重试错误码.",getClassName(method),method.getName());
    		return false;
    	}

    	if(StringUtils.isBlank(errorCodeFiled)) {
    		log.info("请检查类:{}-方法:{}的重试注解是否配置了重试错误码对应的字段.",getClassName(method),method.getName());
    		return false;
    	}

    	List<String> errorCodeList = Arrays.asList(errorCodes);
    	return errorCodeList.contains(CustomReflectionUtils.getValue(method.getReturnType(), errorCodeFiled, result));
    }




    /**
     *
     * 创建重试策略
     *
     * @param methodAnnotation
     * @return
     */
    private RetryInfo.RetryStrategy buildRetryStrategy (FailRetry methodAnnotation) {
    	RetryInfo.RetryStrategy retryStrategy  = new RetryInfo.RetryStrategy();
    	retryStrategy.setRetryErrorCodes(Arrays.asList(methodAnnotation.errorCode()));
    	retryStrategy.setRetryErrorCodeFiled(methodAnnotation.errorCodeFiled());
    	retryStrategy.setRetryTimePoints(CustomCollectionUtils.getListFormStrings(methodAnnotation.retryIntervalTimes()));
    	retryStrategy.setRetryTimeType(methodAnnotation.timeType());

    	return retryStrategy;
    }

    /**
     * 创建重试出参信息
     *
     * @param returnType
     * @return
     */
    private RetryInfo.ResponseInfo buildRetryResponseInfo (Class<?> returnType) {
    	RetryInfo.ResponseInfo responseInfo = new RetryInfo.ResponseInfo();
    	responseInfo.setReponseClass(returnType);

    	return responseInfo;
    }


    /**
     * 获取入参类型和入参值
     *
     * @param method
     * @param args
     * @return
     */
    private List<RetryInfo.RequestInfo> buildRetryRequestInfos(Method method, Object[] args) {
    	 //获取入参的参数
        Class<?>[] parameterClassTypes = method.getParameterTypes();
        int parameterConut = method.getParameterCount();

        if(parameterConut != args.length) {
        	throw new IllegalArgumentException();
        }

    	List<RetryInfo.RequestInfo> targetRequestInfos = new ArrayList<>();
        for(int i=0;i<parameterConut;i++) {
        	RetryInfo.RequestInfo targetRequestInfo = new RetryInfo.RequestInfo();
        	targetRequestInfo.setParameter(args[i]);
        	targetRequestInfo.setRequestClass(parameterClassTypes[i]);
        	targetRequestInfo.setRequestClassName(parameterClassTypes[i].getName());

        	targetRequestInfos.add(targetRequestInfo);
        }

    	return targetRequestInfos;
    }


    /**
     * 判断是否是反射调用的重试
     *
     * @param taskId
     * @return
     */
    private boolean beReflectionInvoke(String taskId) {
    	return !StringUtils.isEmpty(taskId);
    }
}
