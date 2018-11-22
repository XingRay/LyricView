package com.leixing.demo.util;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2018/8/28 19:36
 */
public class TimeUtil {
    private TimeUtil() {
        throw new UnsupportedOperationException();
    }

    public static long[] parseMills(long mills) {
        long[] time = new long[4];
        time[0] =  (mills % 1000);
        long second = (mills / 1000);
        time[1] = second % 60;
        long minute = second / 60;
        time[2] = minute % 60;
        long hours = minute / 60;
        time[3] = hours;
        return time;
    }

    public static String getFormattedMills(long mills) {
        long[] time = parseMills(mills);
        long second = time[1];
        long minute = time[2];
        long hour = time[3];

        StringBuilder builder = new StringBuilder(8);

        if (hour > 0) {
            if (hour < 10) {
                builder.append("0");
            }
            builder.append(hour).append(":");
        }

        if (minute < 10) {
            builder.append("0");
        }
        builder.append(minute).append(":");

        if (second < 10) {
            builder.append("0");
        }
        builder.append(second);
        return builder.toString();
    }
}
