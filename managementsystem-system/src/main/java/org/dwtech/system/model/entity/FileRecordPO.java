package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import org.dwtech.common.base.BaseEntity;
/**
 * FileRecordPO
 *
 * @author steve12311
 * @since 2026-02-26
 */

@Getter
@Setter
@TableName("sys_file_record")
public class FileRecordPO extends BaseEntity {

    /**
     * 对象ID
     */
    private Long objectId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 上传用户ID
     */
    private Long ownerUserId;

    /**
     * 业务类型（预留）
     */
    private String bizType;

    /**
     * 是否删除(0-否 1-是)
     */
    private Integer isDeleted;
}
