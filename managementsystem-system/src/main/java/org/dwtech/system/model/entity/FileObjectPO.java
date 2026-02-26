package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import org.dwtech.common.base.BaseEntity;
/**
 * FileObjectPO
 *
 * @author steve12311
 * @since 2026-02-26
 */

@Getter
@Setter
@TableName("sys_file_object")
public class FileObjectPO extends BaseEntity {

    /**
     * 文件 SHA-256
     */
    private String sha256;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 物理存储相对路径
     */
    private String storagePath;

    /**
     * MIME 类型
     */
    private String mimeType;

    /**
     * 文件后缀（不含点）
     */
    private String ext;

    /**
     * 引用计数
     */
    private Integer refCount;

    /**
     * 是否删除(0-否 1-是)
     */
    private Integer isDeleted;
}
