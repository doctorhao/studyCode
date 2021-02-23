package com.rosenberg.retryqueue.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(RedisTemplate.class)
public class RedisConditionConfiguration {
}
