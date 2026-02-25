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
     * 用途：获取 parameter 信息。
     * 
     * 获取String参数
     * 
     * @param name name
     * @return 结果字符串
     */
    public static String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    /**
     * 用途：获取 parameter 信息。
     * 
     * 获取String参数
     * 
     * @param name name
     * @param defaultValue default value
     * @return 结果字符串
     */
    public static String getParameter(String name, String defaultValue) {
        if (name == null) {
            return defaultValue;
        } else {
            return getRequest().getParameter(name);
        }
    }

    /**
     * 用途：获取 parameter to int 信息。
     * 
     * 获取Integer参数
     * 
     * @param name name
     * @return 数值结果
     */
    public static Integer getParameterToInt(String name) {
        return Integer.parseInt(getRequest().getParameter(name));
    }

    /**
     * 用途：获取 parameter to int 信息。
     * 
     * 获取Integer参数
     * 
     * @param name name
     * @param defaultValue default value
     * @return 数值结果
     */
    public static Integer getParameterToInt(String name, Integer defaultValue) {
        if (name == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(getRequest().getParameter(name));
        }
    }

    /**
     * 用途：获取 parameter to bool 信息。
     * 
     * 获取Boolean参数
     * 
     * @param name name
     * @return 操作结果，true 表示成功，false 表示失败
     */
    public static Boolean getParameterToBool(String name) {
        return Boolean.getBoolean(getRequest().getParameter(name));
    }

    /**
     * 用途：获取 parameter to bool 信息。
     * 
     * 获取Boolean参数
     * 
     * @param name name
     * @param defaultValue default value
     * @return 操作结果，true 表示成功，false 表示失败
     */
    public static Boolean getParameterToBool(String name, Boolean defaultValue) {
        if (name == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(getRequest().getParameter(name));
        }
    }

    /**
     * 用途：获取 request 信息。
     * 
     * 获取request
     * 
     * 入参：无。
     * @return 返回结果
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    /**
     * 用途：获取 response 信息。
     * 
     * 获取response
     * 
     * 入参：无。
     * @return 返回结果
     */
    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }

    /**
     * 用途：获取 session 信息。
     * 
     * 获取session
     * 
     * 入参：无。
     * @return 返回结果
     */
    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    /**
     * 用途：获取 request attributes 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 用途：执行 render string 操作。
     * 
     * 将字符串渲染到客户端
     *
     * @param response 渲染对象
     * @param string   待渲染的字符串
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
     * 用途：校验 agent is mobile。
     * 
     * 判断User-Agent 是不是来自于手机
     * 
     * @param ua ua
     * @return 操作结果，true 表示成功，false 表示失败
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
     * 用途：获取 device fingerprint 信息。
     * 
     * 获取请求设备指纹
     * 
     * 入参：无。
     * @return 结果字符串
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
     * 用途：执行 url encode 操作。
     * 
     * 内容编码
     *
     * @param str 内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * 用途：执行 url decode 操作。
     * 
     * 内容解码
     *
     * @param str 内容
     * @return 解码后的内容
     */
    public static String urlDecode(String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    /**
     * 用途：生成 token。
     * 
     * 生成CSRF Token
     *
     * @return 解码后的内容
     * 入参：无。
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
