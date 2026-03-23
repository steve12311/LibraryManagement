package org.dwtech.common.token;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.OnlineUser;
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

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis Token 管理器
 * <p>
 * 用于生成、解析、校验、刷新 Redis Token
 *
 * @author steve12311
 * @since 2025-11-18
 */
@ConditionalOnProperty(value = "security.session.type", havingValue = "redis-token")
@Service
public class RedisTokenManager implements TokenManager {

    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造 Redis 会话令牌管理器。
     *
     * @param securityProperties 安全配置，提供令牌 TTL 与多端登录策略
     * @param redisTemplate Redis 操作模板
     */
    public RedisTokenManager(SecurityProperties securityProperties, RedisTemplate<String, Object> redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成访问令牌与刷新令牌并写入 Redis。
     *
     * <p>同时维护令牌与用户的双向映射关系，便于刷新、注销和单设备登录控制。</p>
     *
     * @param authentication 用户认证信息
     * @return 令牌响应对象
     */
    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        SysUserDetails user = (SysUserDetails) authentication.getPrincipal();
        String accessToken = IdUtil.fastSimpleUUID();
        String refreshToken = IdUtil.fastSimpleUUID();

        // 构建用户在线信息
        OnlineUser onlineUser = new OnlineUser(
                user.getUserId(),
                user.getUsername(),
                user.getDeptId(),
                user.getDataScope(),
                user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()),
                System.currentTimeMillis()
        );

        // 单设备登录控制
        handleSingleDeviceLogin(user.getUserId());

        // 存储访问令牌、刷新令牌和会话映射
        storeTokensInRedis(accessToken, refreshToken, onlineUser);

        return AuthenticationToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(securityProperties.getSession().getAccessTokenTimeToLive())
                .build();
    }

    /**
     * 根据访问令牌从 Redis 还原认证信息。
     *
     * @param token 访问令牌
     * @return 认证对象；令牌不存在或已过期时返回 {@code null}
     */
    @Override
    public Authentication parseToken(String token) {
        OnlineUser onlineUser = resolveAccessOnlineUser(token);
        if (onlineUser == null) return null;

        // 构建用户权限集合
        Set<SimpleGrantedAuthority> authorities = null;

        Set<String> roles = onlineUser.getRoles();
        if (CollectionUtil.isNotEmpty(roles)) {
            authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        // 构建用户详情对象
        SysUserDetails userDetails = buildUserDetails(onlineUser, authorities);
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    /**
     * 校验访问令牌是否存在于 Redis。
     *
     * @param token 访问令牌
     * @return {@code true} 表示有效
     */
    @Override
    public boolean validateToken(String token) {
        return resolveAccessOnlineUser(token) != null;
    }

    /**
     * 校验刷新令牌是否存在于 Redis。
     *
     * @param refreshToken 刷新令牌
     * @return {@code true} 表示有效
     */
    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return resolveRefreshOnlineUser(refreshToken) != null;
    }

    /**
     * 使用刷新令牌换发新的访问令牌。
     *
     * <p>该方法会清理旧访问令牌映射并写入新令牌，刷新令牌本身保持不变。</p>
     *
     * @param refreshToken 刷新令牌
     * @return 新令牌响应对象
     * @throws BusinessException 刷新令牌不存在或已失效时抛出
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        OnlineUser onlineUser = resolveRefreshOnlineUser(refreshToken);
        if (onlineUser == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        String oldAccessToken = resolveSessionAccessToken(refreshToken, onlineUser.getUserId());

        // 删除旧的访问令牌记录
        if (StrUtil.isNotBlank(oldAccessToken)) {
            redisTemplate.delete(formatTokenKey(oldAccessToken));
        }

        // 生成新访问令牌并存储
        String newAccessToken = IdUtil.fastSimpleUUID();
        storeAccessToken(refreshToken, newAccessToken, onlineUser);

        int accessTtl = securityProperties.getSession().getAccessTokenTimeToLive();
        return AuthenticationToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    /**
     * 按用户维度使其 Redis 会话整体失效。
     *
     * @param userId 用户 ID
     */
    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }
        Set<Object> refreshTokens = redisTemplate.opsForSet().members(formatUserSessionSetKey(userId));
        if (CollectionUtil.isNotEmpty(refreshTokens)) {
            for (Object refreshTokenValue : refreshTokens) {
                String refreshToken = refreshTokenValue == null ? null : String.valueOf(refreshTokenValue);
                clearSession(refreshToken, resolveSessionAccessToken(refreshToken, userId), userId);
            }
        }
        cleanupLegacyUserTokenMappings(userId);

        String invalidAfterKey = StrUtil.format(RedisConstants.Auth.USER_SESSION_INVALID_AFTER, userId);
        long invalidAfter = System.currentTimeMillis();
        int sessionTtl = resolveSessionInvalidationTtl();
        if (sessionTtl == -1) {
            redisTemplate.opsForValue().set(invalidAfterKey, invalidAfter);
            return;
        }
        redisTemplate.opsForValue().set(invalidAfterKey, invalidAfter, sessionTtl, TimeUnit.SECONDS);
    }

    /**
     * 注销访问令牌并清理该用户的令牌映射。
     *
     * @param token 访问令牌
     */
    @Override
    public void invalidateToken(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        OnlineUser accessOnlineUser = resolveAccessOnlineUser(token);
        if (accessOnlineUser != null) {
            Long userId = accessOnlineUser.getUserId();
            String refreshToken = resolveRefreshTokenByAccessToken(userId, token);
            clearSession(refreshToken, token, userId);
            return;
        }

        OnlineUser refreshOnlineUser = resolveRefreshOnlineUser(token);
        if (refreshOnlineUser != null) {
            clearSession(token, resolveSessionAccessToken(token, refreshOnlineUser.getUserId()), refreshOnlineUser.getUserId());
        }
    }

    /**
     * 解析访问令牌对应的在线用户，并校验是否已被用户级失效时间覆盖。
     *
     * @param token 访问令牌
     * @return 在线用户，不存在或已失效时返回 {@code null}
     */
    private OnlineUser resolveAccessOnlineUser(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(formatTokenKey(token));
        if (onlineUser == null || isUserSessionInvalidated(onlineUser)) {
            return null;
        }
        return onlineUser;
    }

    /**
     * 解析刷新令牌对应的在线用户，并校验是否已被用户级失效时间覆盖。
     *
     * @param refreshToken 刷新令牌
     * @return 在线用户，不存在或已失效时返回 {@code null}
     */
    private OnlineUser resolveRefreshOnlineUser(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return null;
        }
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(formatRefreshTokenKey(refreshToken));
        if (onlineUser == null || isUserSessionInvalidated(onlineUser)) {
            return null;
        }
        return onlineUser;
    }

    /**
     * 持久化访问令牌、刷新令牌以及会话维度映射。
     *
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param onlineUser 在线用户信息
     */
    private void storeTokensInRedis(String accessToken, String refreshToken, OnlineUser onlineUser) {
        // 访问令牌 -> 用户信息
        setRedisValue(formatTokenKey(accessToken), onlineUser, securityProperties.getSession().getAccessTokenTimeToLive());

        // 刷新令牌 -> 用户信息
        String refreshTokenKey = StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken);
        setRedisValue(refreshTokenKey, onlineUser, securityProperties.getSession().getRefreshTokenTimeToLive());

        // 刷新令牌 -> 当前访问令牌
        setRedisValue(formatSessionAccessKey(refreshToken),
                accessToken,
                securityProperties.getSession().getRefreshTokenTimeToLive());

        // 用户ID -> 刷新令牌集合
        trackUserSession(onlineUser.getUserId(), refreshToken, securityProperties.getSession().getRefreshTokenTimeToLive());
    }

    /**
     * 根据配置执行单设备登录控制。
     *
     * <p>当不允许多端登录时，会删除当前用户旧会话，保证用户仅保留最新会话。</p>
     *
     * @param userId 用户 ID
     */
    private void handleSingleDeviceLogin(Long userId) {
        Boolean allowMultiLogin = securityProperties.getSession().getRedisToken().getAllowMultiLogin();
        if (Boolean.FALSE.equals(allowMultiLogin)) {
            Set<Object> refreshTokens = redisTemplate.opsForSet().members(formatUserSessionSetKey(userId));
            if (CollectionUtil.isNotEmpty(refreshTokens)) {
                for (Object refreshTokenValue : refreshTokens) {
                    String refreshToken = refreshTokenValue == null ? null : String.valueOf(refreshTokenValue);
                    clearSession(refreshToken, resolveSessionAccessToken(refreshToken, userId), userId);
                }
            }
            cleanupLegacyUserTokenMappings(userId);
        }
    }

    /**
     * 为会话写入新的访问令牌并维护刷新令牌到访问令牌的映射。
     *
     * @param refreshToken 刷新令牌
     * @param newAccessToken 新访问令牌
     * @param onlineUser 在线用户信息
     */
    private void storeAccessToken(String refreshToken, String newAccessToken, OnlineUser onlineUser) {
        setRedisValue(StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, newAccessToken), onlineUser, securityProperties.getSession().getAccessTokenTimeToLive());
        setRedisValue(formatSessionAccessKey(refreshToken), newAccessToken, resolveRefreshSessionTtl(refreshToken));
    }

    /**
     * 将在线用户数据转换为 {@link SysUserDetails}。
     *
     * @param onlineUser 在线用户信息
     * @param authorities 权限集合
     * @return 用户详情对象
     */
    private SysUserDetails buildUserDetails(OnlineUser onlineUser, Set<SimpleGrantedAuthority> authorities) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(onlineUser.getUserId());
        userDetails.setUsername(onlineUser.getUsername());
        userDetails.setDeptId(onlineUser.getDeptId());
        userDetails.setDataScope(onlineUser.getDataScope());
        userDetails.setAuthorities(authorities);
        return userDetails;
    }

    /**
     * 生成访问令牌对应的 Redis key。
     *
     * @param token 访问令牌
     * @return Redis key
     */
    private String formatTokenKey(String token) {
        return StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, token);
    }

    /**
     * 生成刷新令牌对应的 Redis key。
     *
     * @param refreshToken 刷新令牌
     * @return Redis key
     */
    private String formatRefreshTokenKey(String refreshToken) {
        return StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken);
    }

    /**
     * 生成刷新令牌到当前访问令牌映射的 Redis key。
     *
     * @param refreshToken 刷新令牌
     * @return Redis key
     */
    private String formatSessionAccessKey(String refreshToken) {
        return StrUtil.format(RedisConstants.Auth.SESSION_ACCESS_TOKEN, refreshToken);
    }

    /**
     * 生成用户会话集合的 Redis key。
     *
     * @param userId 用户 ID
     * @return Redis key
     */
    private String formatUserSessionSetKey(Long userId) {
        return StrUtil.format(RedisConstants.Auth.USER_SESSION_SET, userId);
    }

    /**
     * 按 TTL 规则写入 Redis。
     *
     * @param key Redis key
     * @param value Redis value
     * @param ttl 过期时间（秒），-1 表示永不过期
     */
    private void setRedisValue(String key, Object value, int ttl) {
        if (ttl != -1) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value); // ttl=-1时永不过期
        }
    }

    /**
     * 将刷新令牌加入用户会话集合，并同步集合 TTL。
     *
     * @param userId 用户 ID
     * @param refreshToken 刷新令牌
     * @param ttl 会话 TTL
     */
    private void trackUserSession(Long userId, String refreshToken, int ttl) {
        if (userId == null || StrUtil.isBlank(refreshToken)) {
            return;
        }
        String userSessionSetKey = formatUserSessionSetKey(userId);
        redisTemplate.opsForSet().add(userSessionSetKey, refreshToken);
        if (ttl != -1) {
            redisTemplate.expire(userSessionSetKey, ttl, TimeUnit.SECONDS);
        }
    }

    /**
     * 读取会话当前绑定的访问令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 访问令牌
     */
    private String resolveSessionAccessToken(String refreshToken) {
        return resolveSessionAccessToken(refreshToken, null);
    }

    /**
     * 读取会话当前绑定的访问令牌，并兼容旧版本用户级单值映射。
     *
     * @param refreshToken 刷新令牌
     * @param userId 用户 ID
     * @return 访问令牌
     */
    private String resolveSessionAccessToken(String refreshToken, Long userId) {
        if (StrUtil.isBlank(refreshToken)) {
            return null;
        }
        Object accessToken = redisTemplate.opsForValue().get(formatSessionAccessKey(refreshToken));
        if (accessToken != null) {
            return String.valueOf(accessToken);
        }
        if (userId == null) {
            return null;
        }
        Object legacyRefreshToken = redisTemplate.opsForValue()
                .get(StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, userId));
        if (legacyRefreshToken != null && StrUtil.equals(refreshToken, String.valueOf(legacyRefreshToken))) {
            Object legacyAccessToken = redisTemplate.opsForValue()
                    .get(StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId));
            return legacyAccessToken == null ? null : String.valueOf(legacyAccessToken);
        }
        return null;
    }

    /**
     * 通过用户会话集合反查访问令牌对应的刷新令牌。
     *
     * @param userId 用户 ID
     * @param accessToken 访问令牌
     * @return 刷新令牌
     */
    private String resolveRefreshTokenByAccessToken(Long userId, String accessToken) {
        if (userId == null || StrUtil.isBlank(accessToken)) {
            return null;
        }
        Set<Object> refreshTokens = redisTemplate.opsForSet().members(formatUserSessionSetKey(userId));
        if (CollectionUtil.isNotEmpty(refreshTokens)) {
            for (Object refreshTokenValue : refreshTokens) {
                String refreshToken = refreshTokenValue == null ? null : String.valueOf(refreshTokenValue);
                if (StrUtil.equals(accessToken, resolveSessionAccessToken(refreshToken))) {
                    return refreshToken;
                }
            }
        }

        Object legacyAccessToken = redisTemplate.opsForValue()
                .get(StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId));
        if (legacyAccessToken != null && StrUtil.equals(accessToken, String.valueOf(legacyAccessToken))) {
            Object legacyRefreshToken = redisTemplate.opsForValue()
                    .get(StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, userId));
            return legacyRefreshToken == null ? null : String.valueOf(legacyRefreshToken);
        }
        return null;
    }

    /**
     * 清理单个会话的访问令牌、刷新令牌和集合映射。
     *
     * @param refreshToken 刷新令牌
     * @param accessToken 访问令牌
     * @param userId 用户 ID
     */
    private void clearSession(String refreshToken, String accessToken, Long userId) {
        if (StrUtil.isNotBlank(accessToken)) {
            redisTemplate.delete(formatTokenKey(accessToken));
        }
        if (StrUtil.isNotBlank(refreshToken)) {
            redisTemplate.delete(formatRefreshTokenKey(refreshToken));
            redisTemplate.delete(formatSessionAccessKey(refreshToken));
            if (userId != null) {
                redisTemplate.opsForSet().remove(formatUserSessionSetKey(userId), refreshToken);
            }
        }
        cleanupLegacyUserTokenMappings(userId);
    }

    /**
     * 清理旧版本的用户级单值映射，避免兼容键干扰新会话模型。
     *
     * @param userId 用户 ID
     */
    private void cleanupLegacyUserTokenMappings(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId));
        redisTemplate.delete(StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, userId));
    }

    /**
     * 判断在线用户对应会话是否被用户级失效时间覆盖。
     *
     * @param onlineUser 在线用户
     * @return {@code true} 表示会话应视为失效
     */
    private boolean isUserSessionInvalidated(OnlineUser onlineUser) {
        if (onlineUser == null || onlineUser.getUserId() == null || onlineUser.getLoginAt() == null) {
            return false;
        }
        Object invalidAfterValue = redisTemplate.opsForValue()
                .get(StrUtil.format(RedisConstants.Auth.USER_SESSION_INVALID_AFTER, onlineUser.getUserId()));
        if (invalidAfterValue == null) {
            return false;
        }
        Long invalidAfter = Long.valueOf(String.valueOf(invalidAfterValue));
        return onlineUser.getLoginAt() <= invalidAfter;
    }

    /**
     * 计算用户级会话失效标记的保留时长。
     *
     * @return TTL 秒数，-1 表示永久保留
     */
    private int resolveSessionInvalidationTtl() {
        int accessTtl = securityProperties.getSession().getAccessTokenTimeToLive();
        int refreshTtl = securityProperties.getSession().getRefreshTokenTimeToLive();
        if (accessTtl == -1 || refreshTtl == -1) {
            return -1;
        }
        return Math.max(accessTtl, refreshTtl);
    }

    /**
     * 读取刷新令牌剩余生存时间，避免刷新访问令牌时意外延长会话总时长。
     *
     * @param refreshToken 刷新令牌
     * @return TTL 秒数，-1 表示永久保留
     */
    private int resolveRefreshSessionTtl(String refreshToken) {
        Long ttl = redisTemplate.getExpire(formatRefreshTokenKey(refreshToken), TimeUnit.SECONDS);
        if (ttl == null) {
            return securityProperties.getSession().getRefreshTokenTimeToLive();
        }
        if (ttl == -1L) {
            return -1;
        }
        if (ttl <= 0L) {
            return securityProperties.getSession().getRefreshTokenTimeToLive();
        }
        return ttl.intValue();
    }
}
