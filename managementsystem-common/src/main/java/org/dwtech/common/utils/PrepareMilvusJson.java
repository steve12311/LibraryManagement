package org.dwtech.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.springframework.stereotype.Component;
/**
 * PrepareMilvusJson
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Component
public class PrepareMilvusJson {
    /**
     * 用途：执行 prepare insert json 操作。
     * 
     * @param id 主键 ID
     * @param vector vector
     * @return 结果字符串
     */
    public String prepareInsertJson(String id, float[] vector) {
        JSONObject object = new JSONObject();
        if (StrUtil.isBlank(id)) {
            throw new BusinessException(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY, "AI 向量同步缺少 ISBN");
        }
        try {
            object.set("id", Long.parseLong(id));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.PARAMETER_FORMAT_MISMATCH, "AI 向量同步仅支持纯数字 ISBN");
        }
        object.set("vector", vector);
        return object.toString();
    }
}
