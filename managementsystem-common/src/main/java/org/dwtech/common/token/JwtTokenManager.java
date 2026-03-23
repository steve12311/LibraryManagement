package org.dwtech.common.token;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import org.apache.commons.lang3.StringUtils;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.constant.JwtClaimConstants;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JWT Token 管理器
 * <p>
 * 用于生成、解析、校验、刷新 JWT Token
 *
 * @author steve12311
 * @since 2025-11-18
 */
@ConditionalOnProperty(value = "security.session.type", havingValue = "jwt")
@Service
public class JwtTokenManager implements TokenManager {

    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final byte[] secretKey;

    /**
     * 构造 JWT 令牌管理器。
     *
     * @param securityProperties 安全配置，提供密钥与令牌 TTL
     * @param redisTemplate Redis 操作模板，用于黑名单校验
     */
    public JwtTokenManager(SecurityProperties securityProperties, RedisTemplate<String, Object> redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
        this.secretKey = securityProperties.getSession().getJwt().getSecretKey().getBytes();
    }

    /**
     * 为已认证用户签发访问令牌与刷新令牌。
     *
     * @param authentication Spring Security 认证信息
     * @return 令牌响应对象，包含访问令牌、刷新令牌和访问令牌过期时间
     */
    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        int accessTokenTimeToLive = securityProperties.getSession().getAccessTokenTimeToLive();
        int refreshTokenTimeToLive = securityProperties.getSession().getRefreshTokenTimeToLive();

        String accessToken = generateToken(authentication, accessTokenTimeToLive);
        String refreshToken = generateToken(authentication, refreshTokenTimeToLive, true);

        return AuthenticationToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenTimeToLive)
                .build();
    }

    /**
     * 解析 JWT 并重建 Spring Security 认证对象。
     *
     * <p>该方法仅负责从 payload 读取用户与权限信息，不执行有效性校验，
     * 调用方应先通过 {@link #validateToken(String)} 或 {@link #validateRefreshToken(String)} 校验。</p>
     *
     * @param token JWT 令牌
     * @return 认证对象
     */
    @Override
    public Authentication parseToken(String token) {

        JWT jwt = JWTUtil.parseToken(token);
        JSONObject payloads = jwt.getPayloads();
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(payloads.getLong(JwtClaimConstants.USER_ID)); // 用户ID
        userDetails.setDeptId(payloads.getLong(JwtClaimConstants.DEPT_ID)); // 部门ID
        userDetails.setDataScope(payloads.getInt(JwtClaimConstants.DATA_SCOPE)); // 数据权限范围

        userDetails.setUsername(payloads.getStr(JWTPayload.SUBJECT)); // 用户名
        // 角色集合
        Set<SimpleGrantedAuthority> authorities = payloads.getJSONArray(JwtClaimConstants.AUTHORITIES)
                .stream()
                .map(authority -> new SimpleGrantedAuthority(Convert.toStr(authority)))
                .collect(Collectors.toSet());

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    /**
     * 校验访问令牌是否有效。
     *
     * @param token JWT 访问令牌
     * @return {@code true} 表示有效，{@code false} 表示无效
     */
    @Override
    public boolean validateToken(String token) {
        return validateToken(token,false);
    }

    /**
     * 校验刷新令牌是否有效。
     *
     * @param refreshToken JWT 刷新令牌
     * @return {@code true} 表示有效，{@code false} 表示无效
     */
    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return validateToken(refreshToken,true);
    }

    /**
     * 统一令牌校验逻辑。
     *
     * <p>校验项包括：签名、过期时间、刷新令牌类型标记（可选）以及 Redis 黑名单状态。</p>
     *
     * @param token JWT 令牌
     * @param validateRefreshToken 是否按刷新令牌模式校验 token 类型
     * @return {@code true} 表示校验通过
     */
    private boolean validateToken(String token, boolean validateRefreshToken) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            // 检查 Token 是否有效(验签 + 是否过期)
            boolean isValid = jwt.setKey(secretKey).validate(0);

            if (isValid) {
                // 检查 Token 是否已被加入黑名单(注销、修改密码等场景)
                JSONObject payloads = jwt.getPayloads();
                String jti = payloads.getStr(JWTPayload.JWT_ID);
                if(validateRefreshToken) {
                    //刷新token需要校验token类别
                    boolean isRefreshToken = payloads.getBool(JwtClaimConstants.TOKEN_TYPE);
                    if (!isRefreshToken) {
                        return false;
                    }
                }
                // 判断是否在黑名单中，如果在，则返回 false 标识Token无效
                if (redisTemplate.hasKey(StrUtil.format(RedisConstants.Auth.BLACKLIST_TOKEN, jti))) {
                    return false;
                }
            }
            return isValid;
        } catch (Exception gitignore) {
            // token 验证
        }
        return false;
    }

    /**
     * 使令牌失效并加入黑名单。
     *
     * <p>黑名单 key 的过期时间与令牌剩余有效期保持一致，避免无意义的长期缓存。</p>
     *
     * @param token JWT 令牌（支持携带或不携带 Bearer 前缀）
     */
    @Override
    public void invalidateToken(String token) {
        if(StringUtils.isBlank(token)) {
            return;
        }

        if (token.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            token = token.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
        }
        JWT jwt = JWTUtil.parseToken(token);
        JSONObject payloads = jwt.getPayloads();
        Integer expirationAt = payloads.getInt(JWTPayload.EXPIRES_AT);
        // 黑名单Token Key
        String blacklistTokenKey = StrUtil.format(RedisConstants.Auth.BLACKLIST_TOKEN, payloads.getStr(JWTPayload.JWT_ID));

        if (expirationAt != null) {
            int currentTimeSeconds = Convert.toInt(System.currentTimeMillis() / 1000);
            if (expirationAt < currentTimeSeconds) {
                // Token已过期，直接返回
                return;
            }
            // 计算Token剩余时间，将其加入黑名单
            int expirationIn = expirationAt - currentTimeSeconds;
            redisTemplate.opsForValue().set(blacklistTokenKey, null, expirationIn, TimeUnit.SECONDS);
        } else {
            // 永不过期的Token永久加入黑名单
            redisTemplate.opsForValue().set(blacklistTokenKey, null);
        }
        ;
    }

    /**
     * 使用刷新令牌换发新的访问令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 新令牌响应对象，返回新访问令牌和新刷新令牌
     * @throws BusinessException 当刷新令牌无效时抛出
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        boolean isValid = validateRefreshToken(refreshToken);
        if (!isValid) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        Authentication authentication = parseToken(refreshToken);
        AuthenticationToken newToken = generateToken(authentication);
        invalidateToken(refreshToken);
        return newToken;
    }

    /**
     * 生成访问令牌（非刷新令牌）。
     *
     * @param authentication 认证信息
     * @param ttl 令牌有效期（秒），-1 表示不过期
     * @return JWT 字符串
     */
    private String generateToken(Authentication authentication, int ttl) {
        return generateToken(authentication, ttl, false);
    }


    /**
     * 生成 JWT 字符串并写入标准与业务声明。
     *
     * @param authentication 认证信息
     * @param ttl 令牌有效期（秒），-1 表示不过期
     * @param isRefreshToken 是否生成刷新令牌
     * @return JWT 字符串
     */
    private String generateToken(Authentication authentication, int ttl, boolean isRefreshToken) {
        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        Map<String, Object> payload = new HashMap<>();
        payload.put(JwtClaimConstants.USER_ID, userDetails.getUserId()); // 用户ID
        payload.put(JwtClaimConstants.DEPT_ID, userDetails.getDeptId()); // 部门ID
        payload.put(JwtClaimConstants.DATA_SCOPE, userDetails.getDataScope()); // 数据权限范围

        // claims 中添加角色信息
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        payload.put(JwtClaimConstants.AUTHORITIES, roles);

        Date now = new Date();
        payload.put(JWTPayload.ISSUED_AT, now);
        payload.put(JwtClaimConstants.TOKEN_TYPE, false);
        if (isRefreshToken) {
            payload.put(JwtClaimConstants.TOKEN_TYPE, true);
        }

        // 设置过期时间 -1 表示永不过期
        if (ttl != -1) {
            Date expiresAt = DateUtil.offsetSecond(now, ttl);
            payload.put(JWTPayload.EXPIRES_AT, expiresAt);
        }
        payload.put(JWTPayload.SUBJECT, authentication.getName());
        payload.put(JWTPayload.JWT_ID, IdUtil.simpleUUID());

        return JWTUtil.createToken(payload, secretKey);
    }

}
