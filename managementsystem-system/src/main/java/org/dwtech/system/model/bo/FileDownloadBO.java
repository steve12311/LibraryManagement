package org.dwtech.system.model.bo;

import lombok.Data;

import java.nio.file.Path;
/**
 * FileDownloadBO
 *
 * @author steve12311
 * @since 2026-02-26
 */

@Data
public class FileDownloadBO {

    /**
     * 文件路径
     */
    private Path filePath;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String mimeType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 是否允许浏览器内联展示
     */
    private boolean inlineAllowed;
}
