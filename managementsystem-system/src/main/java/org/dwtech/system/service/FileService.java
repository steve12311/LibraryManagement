package org.dwtech.system.service;

import org.dwtech.common.core.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;
/**
 * FileService
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface FileService {
    /**
     * 用途：执行 upload file 操作。
     * 
     * 上传文件
     * @param file 表单文件对象
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file);

    /**
     * 用途：删除 file。
     * 
     * 删除文件
     *
     * @param filePath 文件完整URL
     * @return 删除结果
     */
    boolean deleteFile(String filePath);

}
