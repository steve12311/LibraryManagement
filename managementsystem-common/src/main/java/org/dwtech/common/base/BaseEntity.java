package org.dwtech.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * 实体基类 — 提供通用审计字段
 * <p>
 * 子类通过继承获得主键 ID 和自动填充的创建/更新时间。
 * 时间字段由 {@link org.dwtech.common.plugin.MyMetaObjectHandler} 在 MyBatis-Plus 拦截器中自动赋值，
 * INSERT 时填充 {@code createTime} 和 {@code updateTime}，UPDATE 时仅刷新 {@code updateTime}。
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间，仅 INSERT 时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，INSERT 和 UPDATE 时均自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
