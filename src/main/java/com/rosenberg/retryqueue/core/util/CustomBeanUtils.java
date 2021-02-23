package com.rosenberg.retryqueue.core.util;

import java.util.Arrays;
import java.util.List;

/**
 * 用户自定义Bean工具类
 *
 * @author Lenovo
 *
 */
public class CustomBeanUtils {

	/**
         * 根据className获取beanName
     *
     * @param className
     * @return
     */
    public static String getBeanNameFromClassName(String className) {
        String[] classNames =  className.split("\\.");
        List<String> classNameList = Arrays.asList(classNames);
        String beanNameInitialUpper = classNameList.get(classNameList.size()-1);

        return lowerFirst(beanNameInitialUpper);
    }

    /**
     * 将首字母小写  ,移到工具类中
     *
     * @param oldStr
     * @return
     */
    public static String lowerFirst(String oldStr){
        char[]chars = oldStr.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
