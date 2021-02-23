package com.rosenberg.retryqueue.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomCollectionUtils {

    /**
     * 从String数组获取Integer数组
     *
     * @param retryTimes
     * @return
     */
    public static List<Integer> getListFormStrings(String[] retryTimes) {
        List<Integer> retryTimeList = new ArrayList<>();
        List<String> retryTimeStrings = Arrays.asList(retryTimes);
        retryTimeStrings.forEach((retryTime) -> {
            retryTimeList.add(Integer.valueOf(retryTime));
        });

        return retryTimeList;
    }


    public static List<Integer> firstEleRemoveEles (List<Integer> currentDelayTimes) {
        List<Integer> removeFirstDelayTimes = null;
        try {
            removeFirstDelayTimes = new ArrayList<>();
            for(int i=1;i<currentDelayTimes.size();i++) {
                removeFirstDelayTimes.add(currentDelayTimes.get(i));
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        return removeFirstDelayTimes;
    }
}
