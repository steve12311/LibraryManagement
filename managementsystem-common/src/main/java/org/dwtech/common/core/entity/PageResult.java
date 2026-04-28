package org.dwtech.common.core.entity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import org.dwtech.common.enmus.ResultCode;

import java.io.Serializable;
import java.util.List;

/**
 * 分页 API 响应体
 * <p>
 * 所有分页查询接口统一返回此结构。内嵌 {@link Data} 承载当前页记录列表 {@code list} 和总记录数 {@code total}，
 * 前端据此渲染分页组件。
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class PageResult<T> implements Serializable {

    /** 业务状态码 */
    private String code;

    /** 分页数据容器，包含当前页列表和总记录数 */
    private Data<T> data;

    /** 响应消息 */
    private String msg;

    /** 从 MyBatis-Plus 分页对象构建成功响应 */
    public static <T> PageResult<T> success(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());

        Data<T> data = new Data<>();
        data.setList(page.getRecords());
        data.setTotal(page.getTotal());

        result.setData(data);
        result.setMsg(ResultCode.SUCCESS.getMsg());
        return result;
    }

    /** 分页数据内部容器 */
    @lombok.Data
    public static class Data<T> {

        /** 当前页记录列表 */
        private List<T> list;

        /** 总记录数，用于前端计算总页数 */
        private long total;

    }

}
