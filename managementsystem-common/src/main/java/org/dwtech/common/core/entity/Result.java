package org.dwtech.common.core.entity;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dwtech.common.enmus.ResultCode;

import java.io.Serializable;

/**
 * 统一响应结构体
 *
 * @author steve12311
* @since 2025-11-18
 **/
/**
 * Result
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class Result<T> implements Serializable {

    private String code;

    private T data;

    private String msg;

    /**
     * 用途：执行 success 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 用途：执行 success 操作。
     * 
     * @param data data
     * @return 返回结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    /**
     * 用途：执行 failed 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    public static <T> Result<T> failed() {
        return result(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMsg(), null);
    }

    /**
     * 用途：执行 failed 操作。
     * 
     * @param msg msg
     * @return 返回结果
     */
    public static <T> Result<T> failed(String msg) {
        return result(ResultCode.SYSTEM_ERROR.getCode(), msg, null);
    }

    /**
     * 用途：执行 judge 操作。
     * 
     * @param status status
     * @return 返回结果
     */
    public static <T> Result<T> judge(boolean status) {
        if (status) {
            return success();
        } else {
            return failed();
        }
    }

    /**
     * 用途：执行 failed 操作。
     * 
     * @param resultCode result code
     * @return 返回结果
     */
    public static <T> Result<T> failed(IResultCode resultCode) {
        return result(resultCode.getCode(), resultCode.getMsg(), null);
    }

    /**
     * 用途：执行 failed 操作。
     * 
     * @param resultCode result code
     * @param msg msg
     * @return 返回结果
     */
    public static <T> Result<T> failed(IResultCode resultCode, String msg) {
        return result(resultCode.getCode(), StrUtil.isNotBlank(msg) ? msg : resultCode.getMsg(), null);
    }

    /**
     * 用途：执行 result 操作。
     * 
     * @param resultCode result code
     * @param data data
     * @return 返回结果
     */
    private static <T> Result<T> result(IResultCode resultCode, T data) {
        return result(resultCode.getCode(), resultCode.getMsg(), data);
    }

    /**
     * 用途：执行 result 操作。
     * 
     * @param code code
     * @param msg msg
     * @param data data
     * @return 返回结果
     */
    private static <T> Result<T> result(String code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setData(data);
        result.setMsg(msg);
        return result;
    }

    /**
     * 用途：判断 success 状态。
     * 
     * @param result result
     * @return 操作结果，true 表示成功，false 表示失败
     */
    public static boolean isSuccess(Result<?> result) {
        return result != null && ResultCode.SUCCESS.getCode().equals(result.getCode());
    }
}
