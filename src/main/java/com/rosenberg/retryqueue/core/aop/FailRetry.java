package com.rosenberg.retryqueue.core.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FailRetry {
    boolean isStart() default true;  //是否开启重试
    String[] retryIntervalTimes () default {};   //重试的时间间隔点
    String[] errorCode() default {};     //需要重试的错误码值
    String errorCodeFiled() default "";  //由于目前的出参没有统一定义结构,需要加上该字段,错误码是哪个字段
    TimeType timeType() default TimeType.MINUTE;  //重试时间类型 默认分
}
