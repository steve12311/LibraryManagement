package org.dwtech.common.core.entity;

/**
 * 响应码接口
 *
 * @author steve12311
 * @since 2025-11-18
 **/
public interface IResultCode {

    /**
     * 获取响应状态码。
     *
     * @return 响应状态码
     */
    String getCode();

    /**
     * 获取响应消息描述。
     *
     * @return 响应消息描述
     */
    String getMsg();

}
