package org.dwtech.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 客户端工具类
 *
 * @author steve12311
 * @since 2025-10-30
 */
@Slf4j
public class ServletUtils {
    /**
     * 定义移动端请求的所有可能类型
     */
    private final static String[] agent = {"Android", "iPhone", "iPod", "iPad", "Windows Phone", "MQQBrowser"};

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 获取 String 类型请求参数。
     *
     * @param name 参数名
     * @return 参数值字符串
     */
    public static String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    /**
     * 获取 String 类型请求参数，为空时返回默认值。
     *
     * @param name         参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public static String getParameter(String name, String defaultValue) {
        if (name == null) {
            return defaultValue;
        } else {
            return getRequest().getParameter(name);
        }
    }

    /**
     * 获取 Integer 类型请求参数。
     *
     * @param name 参数名
     * @return 整型参数值
     */
    public static Integer getParameterToInt(String name) {
        return Integer.parseInt(getRequest().getParameter(name));
    }

    /**
     * 获取 Integer 类型请求参数，为空时返回默认值。
     *
     * @param name         参数名
     * @param defaultValue 默认值
     * @return 整型参数值或默认值
     */
    public static Integer getParameterToInt(String name, Integer defaultValue) {
        if (name == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(getRequest().getParameter(name));
        }
    }

    /**
     * 获取 Boolean 类型请求参数。
     *
     * @param name 参数名
     * @return 布尔型参数值
     */
    public static Boolean getParameterToBool(String name) {
        return Boolean.getBoolean(getRequest().getParameter(name));
    }

    /**
     * 获取 Boolean 类型请求参数，为空时返回默认值。
     *
     * @param name         参数名
     * @param defaultValue 默认值
     * @return 布尔型参数值或默认值
     */
    public static Boolean getParameterToBool(String name, Boolean defaultValue) {
        if (name == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(getRequest().getParameter(name));
        }
    }

    /**
     * 获取当前请求的 HttpServletRequest 对象。
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    /**
     * 获取当前请求的 HttpServletResponse 对象。
     *
     * @return HttpServletResponse
     */
    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }

    /**
     * 获取当前请求的 HttpSession 对象。
     *
     * @return HttpSession
     */
    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    /**
     * 获取当前请求的 ServletRequestAttributes。
     *
     * @return ServletRequestAttributes
     */
    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 将 JSON 字符串写入 HTTP 响应并返回给客户端。
     *
     * @param response HTTP 响应对象
     * @param string   待渲染的 JSON 字符串
     * @return null
     */
    public static String renderString(HttpServletResponse response, String string) {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 判断 User-Agent 是否来自移动端设备。
     *
     * @param ua User-Agent 字符串
     * @return 移动端返回 true，否则返回 false
     */
    public static boolean checkAgentIsMobile(String ua) {
        boolean flag = false;
        if (!ua.contains("Windows NT") || (ua.contains("Windows NT") && ua.contains("compatible; MSIE 9.0;"))) {
            // 排除 苹果桌面系统
            if (!ua.contains("Windows NT") && !ua.contains("Macintosh")) {
                for (String item : agent) {
                    if (ua.contains(item)) {
                        flag = true;
                        break;
                    }
                }
            }
        }
        return flag;
    }

    /**
     * 根据请求头信息计算设备指纹（哈希值）。
     *
     * @return 设备指纹十六进制字符串
     */
    public static String getDeviceFingerprint() {
        HttpServletRequest request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();
        String accept = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        String deviceFingerprint = userAgent + ipAddress + accept + acceptLanguage + acceptEncoding;
        return Integer.toHexString(deviceFingerprint.hashCode());
    }

    /**
     * 对字符串进行 URL 编码（UTF-8）。
     *
     * @param str 待编码内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * 对字符串进行 URL 解码（UTF-8）。
     *
     * @param str 待解码内容
     * @return 解码后的内容
     */
    public static String urlDecode(String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    /**
     * 生成安全的 CSRF Token（Base64 编码）。
     *
     * @return CSRF Token
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
