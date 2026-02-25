package org.dwtech.common.utils;

import cn.hutool.json.JSONObject;
import org.springframework.stereotype.Component;
/**
 * PrepareMilvusJson
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Component
public class PrepareMilvusJson {
    public String prepareInsertJson(String id, float[] vector) {
        JSONObject object = new JSONObject();
        object.set("id", Long.parseLong(id));
        object.set("vector", vector);
        return object.toString();
    }
}
