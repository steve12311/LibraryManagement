package org.dwtech.common.core.entity;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dwtech.common.enmus.ResultCode;

import java.io.Serializable;

/**
 * 统一 API 响应体（单项数据）
 * <p>
 * 所有非分页接口均返回此结构，包含业务状态码 {@code code}、消息 {@code msg} 和泛型数据体 {@code data}。
 * 通过静态工厂方法构造，避免直接 new，保证响应格式一致。
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class Result<T> implements Serializable {

    /** 业务状态码，参考 {@link ResultCode} */
    private String code;

    /** 响应数据体，泛型承载任意业务对象 */
    private T data;

    /** 响应消息，成功时为空或描述文本，失败时为错误原因 */
    private String msg;

    /** 返回成功结果（无数据体） */
    public static <T> Result<T> success() {
        return success(null);
    }

    /** 返回成功结果（携带数据体） */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    /** 返回默认失败结果 */
    public static <T> Result<T> failed() {
        return result(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMsg(), null);
    }

    /** 返回失败结果（自定义消息） */
    public static <T> Result<T> failed(String msg) {
        return result(ResultCode.SYSTEM_ERROR.getCode(), msg, null);
    }

    /** 根据布尔值返回成功或失败，用于简单判断场景 */
    public static <T> Result<T> judge(boolean status) {
        if (status) {
            return success();
        } else {
            return failed();
        }
    }

    /** 返回失败结果（使用预定义错误码） */
    public static <T> Result<T> failed(IResultCode resultCode) {
        return result(resultCode.getCode(), resultCode.getMsg(), null);
    }

    /** 返回失败结果（预定义错误码 + 自定义消息覆盖） */
    public static <T> Result<T> failed(IResultCode resultCode, String msg) {
        return result(resultCode.getCode(), StrUtil.isNotBlank(msg) ? msg : resultCode.getMsg(), null);
    }

    /** 内部工具方法：通过 IResultCode 构建 Result */
    private static <T> Result<T> result(IResultCode resultCode, T data) {
        return result(resultCode.getCode(), resultCode.getMsg(), data);
    }

    /** 内部工具方法：通过 code/msg/data 三要素构建 Result */
    private static <T> Result<T> result(String code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setData(data);
        result.setMsg(msg);
        return result;
    }

    /** 判断响应是否成功，常用于切面或调用方快速判断 */
    public static boolean isSuccess(Result<?> result) {
        return result != null && ResultCode.SUCCESS.getCode().equals(result.getCode());
    }
}
