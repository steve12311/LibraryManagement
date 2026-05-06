package org.dwtech.system.service;

import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口，提供文件上传、下载信息获取和删除功能。
 * 可根据配置切换本地存储或对象存储实现。
 *
 * @author steve12311
 * @since 2026-02-22
 */
public interface FileService {

    /**
     * 上传文件。校验文件类型和大小，计算 SHA-256 指纹进行去重存储，
     * 写入文件记录并返回访问链接。
     *
     * @param file 待上传的文件
     * @return 文件信息（文件名、访问 URL）
     */
    FileInfo uploadFile(MultipartFile file);

    /**
     * 根据文件 ID 获取文件下载信息，含文件路径、文件名、MIME 类型和大小。
     *
     * @param fileId 文件 ID
     * @return 文件下载信息
     */
    FileDownloadBO getFile(Long fileId);

    /**
     * 物理删除文件记录（需调用方校验 sys:file:del 权限）。
     * 删除文件记录，文件对象引用计数减一，引用归零时删除物理文件。
     *
     * @param fileId 文件 ID
     * @return true 表示删除成功，false 表示文件不存在
     */
    boolean deleteFilePhysical(Long fileId);

    /**
     * 引用计数删除文件记录。删除文件记录，文件对象引用计数减一，
     * 仅当引用归零时才删除物理文件。供消息队列异步调用。
     *
     * @param fileId 文件 ID
     * @return true 表示删除成功，false 表示文件不存在
     */
    boolean deleteFileByRefCount(Long fileId);

    /**
     * 获取文件对象的引用计数，供前端删除确认弹窗查询。
     *
     * @param fileId 文件 ID
     * @return 引用计数，文件不存在时返回 0
     */
    int getFileRefCount(Long fileId);
}
