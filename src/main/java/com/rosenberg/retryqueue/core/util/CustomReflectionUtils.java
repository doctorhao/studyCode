package com.rosenberg.retryqueue.core.util;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class CustomReflectionUtils {
    /**
     * 反射获取某个字段的值
     *
     * @param returnType
     * @param errorCodeFiled
     * @param result
     * @return
     */
    public static String getValue(Class<?> returnType,String errorCodeFiled, Object result) {
        Object filedValue = null;
        try {
            Field filed = ReflectionUtils.findField(returnType, errorCodeFiled);
            filed.setAccessible(Boolean.TRUE);
            filedValue = filed.get(result);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "";
        }

        return filedValue.toString();
    }
}
