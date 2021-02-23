package com.rosenberg.retryqueue.config;

import com.rosenberg.retryqueue.core.server.DelayWorkServer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.rosenberg.retryqueue.core.aop.FailRetryIntercept;
import com.rosenberg.retryqueue.core.client.DelayWorkClient;

@Configuration
@AutoConfigureAfter({RedisConditionConfiguration.class})
@ConditionalOnProperty(prefix = "retry.task", name = "enabled", havingValue = "true")
@ComponentScan({
        "com.rosenberg.retryqueue.core",
        "com.rosenberg.retryqueue.domain",
        "com.rosenberg.retryqueue.exception"
})
public class RetryTaskAutoConfiguration {
	@Value("${spring.application.name}")
	private String applicationName;

	private String defaultAppName = "DEFAULT";

    @Bean
    public DelayWorkClient delayWorkClient() {
        return new DelayWorkClient();
    }

    @Bean
    public DelayWorkServer delayWorkServer() {
        return new DelayWorkServer();
    }

    @Bean
    public FailRetryIntercept failRetryIntercept() {
    	return new FailRetryIntercept();
    }

    /**
	  *   创建重试队列分组， 该名称必须要唯一
	 *
	 * @return
	 */
	public String buildAppName() {
		if(StringUtils.isNotBlank(applicationName)) {
			return applicationName.toUpperCase();
		}

		return defaultAppName;
	}
}
