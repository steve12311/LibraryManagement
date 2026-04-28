package org.dwtech.common.plugin;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>
 * 配合 {@link org.dwtech.common.base.BaseEntity} 使用：
 * <ul>
 *   <li>INSERT 时自动填充 {@code createTime} 和 {@code updateTime}</li>
 *   <li>UPDATE 时自动刷新 {@code updateTime}</li>
 * </ul>
 * 业务代码无需手动设置时间字段。
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** 新增时自动填充创建时间和更新时间 */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

    /** 更新时自动填充更新时间 */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

}
