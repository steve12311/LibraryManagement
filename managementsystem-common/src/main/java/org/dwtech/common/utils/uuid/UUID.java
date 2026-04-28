package org.dwtech.common.utils.uuid;

import java.security.SecureRandom;
/**
 * UUID 记录类，提供版本 4 随机 UUID 的生成与格式化。
 *
 * @author steve12311
 * @since 2025-10-30
 * @param mostSigBits  最高有效 64 位
 * @param leastSigBits 最低有效 64 位
 */

public record UUID(long mostSigBits, long leastSigBits) implements java.io.Serializable, Comparable<UUID> {
    private static final SecureRandom numberGenerator = new SecureRandom();

    /**
     * 生成版本 4 随机 UUID。
     *
     * @return 随机 UUID 实例
     */
    public static UUID randomUUID() {
        byte[] randomBytes = new byte[16];
        numberGenerator.nextBytes(randomBytes);

        // 设置版本号为4（随机UUID）
        randomBytes[6] &= 0x0f;  // 清高高4位
        randomBytes[6] |= 0x40;  // 设置为版本4

        // 设置变体为10xx
        randomBytes[8] &= 0x3f;  // 清高高2位
        randomBytes[8] |= (byte) 0x80;  // 设置为变体1

        return createFromBytes(randomBytes);
    }

    /**
     * 从 16 字节数组创建 UUID。
     *
     * @param data 16 字节数组
     * @return UUID 实例
     */
    private static UUID createFromBytes(byte[] data) {
        long msb = 0;
        long lsb = 0;

        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (data[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (data[i] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    /**
     * 返回标准的 UUID 字符串表示（带横线）。
     *
     * @return UUID 字符串
     */
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * 返回指定数字对应的十六进制字符串。
     *
     * @param val    数值
     * @param digits 十六进制位数
     * @return 十六进制字符串
     */
    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    /**
     * 返回 UUID 字符串表示，可选择是否去掉横线。
     *
     * @param isSimple true 返回无横线格式，false 返回标准格式
     * @return UUID 字符串
     */
    public String toString(boolean isSimple) {
        final StringBuilder builder = new StringBuilder(isSimple ? 32 : 36);
        // time_low
        builder.append(digits(mostSigBits >> 32, 8));
        if (!isSimple) {
            builder.append('-');
        }
        // time_mid
        builder.append(digits(mostSigBits >> 16, 4));
        if (!isSimple) {
            builder.append('-');
        }
        // time_high_and_version
        builder.append(digits(mostSigBits, 4));
        if (!isSimple) {
            builder.append('-');
        }
        // variant_and_sequence
        builder.append(digits(leastSigBits >> 48, 4));
        if (!isSimple) {
            builder.append('-');
        }
        // node
        builder.append(digits(leastSigBits, 12));

        return builder.toString();
    }

    /**
     * 将 long 值转换为十六进制字符串并填充到字符数组中。
     *
     * @param val    数值
     * @param digits 十六进制位数
     * @param buf    目标字符数组
     * @param offset 填充起始偏移
     */
    private static void digits(long val, int digits, char[] buf, int offset) {
        long hi = 1L << (digits * 4);
        String hex = Long.toHexString(hi | (val & (hi - 1)));
        hex.getChars(1, hex.length(), buf, offset);
    }

    /**
     * 获取最高有效 64 位。
     *
     * @return 最高有效 64 位值
     */
    public long getMostSignificantBits() {
        return mostSigBits;
    }

    /**
     * 获取最低有效 64 位。
     *
     * @return 最低有效 64 位值
     */
    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    /**
     * 基于高低 64 位计算哈希码。
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        long hilo = mostSigBits ^ leastSigBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    /**
     * 比较两个 UUID 的大小（按最高有效位优先）。
     *
     * @param other 待比较的 UUID
     * @return 负数、零或正数
     */
    public int compareTo(UUID other) {
        if (this.mostSigBits != other.mostSigBits) {
            return Long.compare(this.mostSigBits, other.mostSigBits);
        }
        return Long.compare(this.leastSigBits, other.leastSigBits);
    }
}