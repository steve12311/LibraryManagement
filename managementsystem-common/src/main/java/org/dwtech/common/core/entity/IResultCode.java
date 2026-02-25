package org.dwtech.common.core.entity;

/**
 * 响应码接口
 *
 * @author steve12311
 * @since 2025-11-18
 **/
public interface IResultCode {

    /**
     * 用途：获取 code 信息。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String getCode();

    /**
     * 用途：获取 msg 信息。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String getMsg();

}
