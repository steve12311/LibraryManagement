package org.dwtech.common.utils;

import java.util.Date;

public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    /**
     * 计算相差天数
     */
    public static int differentDaysByMillisecond(Date date1, Date date2)
    {
        return Math.abs((int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24)));
    }
}
