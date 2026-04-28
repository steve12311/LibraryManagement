package org.dwtech.common.utils.uuid;

/**
 * ID生成器工具类
 *
 * @author steve12311
 * @since 2025-10-30
 */
public class IdUtils {
    /**
     * 生成随机 UUID（带横线）。
     *
     * @return 随机 UUID 字符串
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成简化的随机 UUID（去掉横线）。
     *
     * @return 无横线的 UUID 字符串
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString(true);
    }
}
