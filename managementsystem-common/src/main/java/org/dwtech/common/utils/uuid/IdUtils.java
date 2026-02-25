package org.dwtech.common.utils.uuid;

/**
 * ID生成器工具类
 *
 * @author steve12311
 * @since 2025-10-30
 */
public class IdUtils {
    /**
     * 用途：执行 random uuid 操作。
     * 
     * 获取随机UUID
     *
     * @return 随机UUID
     * 入参：无。
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 用途：执行 simple uuid 操作。
     * 
     * 简化的UUID，去掉了横线
     *
     * @return 简化的UUID，去掉了横线
     * 入参：无。
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString(true);
    }
}
