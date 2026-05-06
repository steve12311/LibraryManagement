package org.dwtech.system.model.bo;

import lombok.Data;
/**
 * FileMetaCacheBO
 *
 * @author steve12311
 * @since 2026-02-26
 */

@Data
public class FileMetaCacheBO {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 对象ID
     */
    private Long objectId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件所属用户ID
     */
    private Long ownerUserId;

    /**
     * 存储相对路径
     */
    private String storagePath;

    /**
     * 文件类型
     */
    private String mimeType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件哈希
     */
    private String sha256;
}
