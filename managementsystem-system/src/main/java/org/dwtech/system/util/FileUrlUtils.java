package org.dwtech.system.util;

import cn.hutool.core.util.StrUtil;

/**
 * FileUrlUtils
 *
 * @author steve12311
 * @since 2026-05-05
 */
public final class FileUrlUtils {

    private static final String FILE_API_PREFIX = "/api/v1/files/";

    private FileUrlUtils() {
    }

    /**
     * 从文件URL中提取文件记录ID。
     * 支持格式："/{fileId}"、"/api/v1/files/{fileId}"、"api/v1/files/{fileId}"
     *
     * @param url 文件URL
     * @return 文件ID，解析失败返回 null
     */
    public static Long extractFileId(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        String trimmed = url.trim();
        String path;
        if (trimmed.startsWith(FILE_API_PREFIX)) {
            path = trimmed.substring(FILE_API_PREFIX.length());
        } else if (trimmed.startsWith("/api/v1/files/")) {
            path = trimmed.substring("/api/v1/files/".length());
        } else if (trimmed.startsWith("api/v1/files/")) {
            path = trimmed.substring("api/v1/files/".length());
        } else if (trimmed.startsWith("/")) {
            path = trimmed.substring(1);
        } else {
            path = trimmed;
        }
        int slashIdx = path.indexOf('/');
        if (slashIdx != -1) {
            path = path.substring(0, slashIdx);
        }
        try {
            return Long.parseLong(path);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
