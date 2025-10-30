package org.dwtech.common.utils;

import org.dwtech.common.core.entity.Condition;
import org.dwtech.common.exception.ServiceException;

import java.text.SimpleDateFormat;

public class PageUtils {
    public static Integer getPageStart() {
        return Convert.toInt(ServletUtils.getParameter("pageNum"), 1);
    }

    public static Integer getPageSize() {
        return Convert.toInt(ServletUtils.getParameter("pageSize"), 10);
    }

    public static Condition getCondition() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Condition condition = new Condition();
        String startTime = ServletUtils.getParameter("startTime");
        String endTime = ServletUtils.getParameter("endTime");
        try {
            if (startTime != null && endTime != null) {
                condition.setBeginTime(formatter.parse(startTime));
                condition.setEndTime(formatter.parse(endTime));
            }
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        return condition;
    }
}
