package com.rosenberg.retryqueue.domain;

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.rosenberg.retryqueue.core.aop.TimeType;

import lombok.Data;

@Data
public class RetryInfo {
	private String appName;
	private String consumeBeanName="failRetryConsumer";  //消费者beanName，目前只有一个消费者，后续可以考虑分组
	private String retryBeanName; //重试调用的BeanName;
	private String retryMethodName; //重试调用的methodName;
	private RetryStrategy retryStrategy;  //重试策略
	private List<RequestInfo> targetRequestInfos;  //目标方法的入参信息
	private ResponseInfo targetReponseInfo;  //目标方法的返回信息

	@Data
	public static class RetryStrategy {
		private List<Integer> retryTimePoints; //重试时间点　
		private TimeType retryTimeType; //时间类型
		private List<String> retryErrorCodes; //重试的错误码值
		private String retryErrorCodeFiled;  //支持错误码字段没有统一的情况
	}

	@Data
	public static class RequestInfo {
		private String requestClassName; //入参类型名称
		private Class<?> requestClass;  //入参类型
		private Object parameter;  //入参内容
	}

	@Data
	public static class ResponseInfo {
		private Class<?> reponseClass;  //出参类型
	}

	public static RetryInfoBuilder builder() {
	        return new RetryInfoBuilder();
	}

	public RetryInfo() {}

	public RetryInfo(String appName, String retryBeanName, String retryMethodName,
				List<RetryInfo.RequestInfo> targetRequestInfos, RetryInfo.ResponseInfo targetReponseInfo, RetryInfo.RetryStrategy retryStrategy) {
		 Assert.isTrue(!StringUtils.isEmpty(appName), "重试应用名称不能为空");
		 Assert.isTrue(!StringUtils.isEmpty(retryBeanName), "重试类的名称不能为空");
		 Assert.isTrue(!StringUtils.isEmpty(retryMethodName), "重试方法不能为空");
		 Assert.isTrue(!CollectionUtils.isEmpty(targetRequestInfos), "重试方法不能为空");
		 Assert.notNull(targetReponseInfo, "目标函数出参信息不能为null");
		 Assert.notNull(retryStrategy, "重试策略不能null");

		 this.appName = appName;
		 this.retryBeanName = retryBeanName;
		 this.retryMethodName = retryMethodName;
		 this.targetRequestInfos = targetRequestInfos;
		 this.targetReponseInfo = targetReponseInfo;
		 this.retryStrategy = retryStrategy;
	}

    public static class RetryInfoBuilder {
    	private String appName;
        private String retryBeanName;
    	private String retryMethodName;
        private List<RetryInfo.RequestInfo> targetRequestInfos;
        private RetryInfo.ResponseInfo targetReponseInfo;
        private RetryInfo.RetryStrategy retryStrategy;

        public RetryInfoBuilder() {}

        public RetryInfoBuilder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public RetryInfoBuilder retryBeanName(String retryBeanName) {
            this.retryBeanName = retryBeanName;
            return this;
        }

        public RetryInfoBuilder retryMethodName(String retryMethodName) {
            this.retryMethodName = retryMethodName;
            return this;
        }

        public RetryInfoBuilder targetRequestInfos(List<RetryInfo.RequestInfo> targetRequestInfos) {
            this.targetRequestInfos = targetRequestInfos;
            return this;
        }

        public RetryInfoBuilder targetResponseInfo(RetryInfo.ResponseInfo targetReponseInfo) {
            this.targetReponseInfo = targetReponseInfo;
            return this;
        }

        public RetryInfoBuilder retryStrategy(RetryInfo.RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }

        public RetryInfo build() {
            return new RetryInfo(appName, retryBeanName, retryMethodName,targetRequestInfos, targetReponseInfo, retryStrategy);
        }
    }
}
