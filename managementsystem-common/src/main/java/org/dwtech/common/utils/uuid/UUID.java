package org.dwtech.common.utils.uuid;

import java.security.SecureRandom;
/**
 * UUID
 *
 * @author steve12311
 * @since 2025-10-30
 */

public record UUID(long mostSigBits, long leastSigBits) implements java.io.Serializable, Comparable<UUID> {
    private static final SecureRandom numberGenerator = new SecureRandom();

    /**
     * 生成随机UUID
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
     * 从字节数组创建UUID
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
     * 返回标准的UUID字符串表示（带横线）
     */
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * 返回指定数字对应的hex值
     *
     * @param val    值
     * @param digits 位
     * @return 值
     */
    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    /**
     * 返回UUID字符串表示
     *
     * @param isSimple 如果为true，则去掉横线
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
     * 将long值转换为十六进制字符串并填充到字符数组中
     */
    private static void digits(long val, int digits, char[] buf, int offset) {
        long hi = 1L << (digits * 4);
        String hex = Long.toHexString(hi | (val & (hi - 1)));
        hex.getChars(1, hex.length(), buf, offset);
    }

    /**
     * 获取最高有效64位
     */
    public long getMostSignificantBits() {
        return mostSigBits;
    }

    /**
     * 获取最低有效64位
     */
    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    /**
     * 生成哈希码
     */
    @Override
    public int hashCode() {
        long hilo = mostSigBits ^ leastSigBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    /**
     * 比较两个UUID
     */
    public int compareTo(UUID other) {
        if (this.mostSigBits != other.mostSigBits) {
            return Long.compare(this.mostSigBits, other.mostSigBits);
        }
        return Long.compare(this.leastSigBits, other.leastSigBits);
    }
}