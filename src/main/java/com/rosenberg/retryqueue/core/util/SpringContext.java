package com.rosenberg.retryqueue.core.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * SpringContext 静态访问类,用于获取Spring 依赖的注入
 *
 */
@Service
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContext.context = applicationContext;
    }

    public static Object getBean(String beanId) {
        return context.getBean(beanId);
    }

    /**
     * 根据类型获取bean
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    /**
     * 根据beanId获取具体的class实例
     *
     * @param beanId
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(String beanId, Class<T> clazz) {
        return context.getBean(beanId, clazz);
    }

    /**
     * @return the applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return context;
    }

}
