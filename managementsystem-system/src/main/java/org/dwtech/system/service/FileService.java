package org.dwtech.system.service;

import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.system.model.bo.FileDownloadBO;
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
     * 用途：根据 fileId 获取文件下载信息。
     *
     * @param fileId 文件ID
     * @return 文件下载信息
     */
    FileDownloadBO getFile(Long fileId);

    /**
     * 用途：删除 file。
     *
     * @param fileId 文件ID
     * @return 删除结果
     */
    boolean deleteFile(Long fileId);

}
