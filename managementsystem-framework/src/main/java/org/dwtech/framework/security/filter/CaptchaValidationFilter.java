package org.dwtech.framework.security.filter;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.utils.WebResponseHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * 图形验证码校验过滤器
 *
 * @author steve12311
 * @since 2025-11-18
 */
public class CaptchaValidationFilter extends OncePerRequestFilter {

    private static final RequestMatcher LOGIN_PATH_REQUEST_MATCHER = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, SecurityConstants.LOGIN_PATH);

    public static final String CAPTCHA_CODE_PARAM_NAME = "captchaCode";
    public static final String CAPTCHA_KEY_PARAM_NAME = "captchaKey";

    private final RedisTemplate<String, Object> redisTemplate;

    private final CodeGenerator codeGenerator;

    /**
     * 用途：创建 CaptchaValidationFilter 实例。
     * 
     * @param redisTemplate redis template
     * @param codeGenerator code generator
     * 返回：无。
     */
    public CaptchaValidationFilter(RedisTemplate<String, Object> redisTemplate, CodeGenerator codeGenerator) {
        this.redisTemplate = redisTemplate;
        this.codeGenerator = codeGenerator;
    }


    /**
     * 用途：执行 do filter internal 操作。
     * 
     * @param request request
     * @param response response
     * @param chain chain
     * 返回：无。
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 检验登录接口的验证码
        if (LOGIN_PATH_REQUEST_MATCHER.matches(request)) {
            // 请求中的验证码
            String captchaCode = request.getParameter(CAPTCHA_CODE_PARAM_NAME);
            String verifyCodeKey = request.getParameter(CAPTCHA_KEY_PARAM_NAME);
            if (StrUtil.isBlank(captchaCode) || StrUtil.isBlank(verifyCodeKey)) {
                WebResponseHelper.writeError(response, ResultCode.USER_VERIFICATION_CODE_ERROR);
                return;
            }
            // 缓存中的验证码
            String captchaCacheKey = StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, verifyCodeKey);
            String cacheVerifyCode = (String) redisTemplate.opsForValue().get(captchaCacheKey);
            // 一次性验证码，取出后立即删除，避免暴力重试和重放
            redisTemplate.delete(captchaCacheKey);
            if (cacheVerifyCode == null) {
                WebResponseHelper.writeError(response, ResultCode.USER_VERIFICATION_CODE_EXPIRED);
            } else {
                // 验证码比对
                if (codeGenerator.verify(cacheVerifyCode, captchaCode)) {
                    chain.doFilter(request, response);
                } else {
                    WebResponseHelper.writeError(response, ResultCode.USER_VERIFICATION_CODE_ERROR);
                }
            }
        } else {
            // 非登录接口放行
            chain.doFilter(request, response);
        }
    }

}
