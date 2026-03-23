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
                        .collect(Collectors.toSet())
        );

        // 存储访问令牌、刷新令牌和刷新令牌映射
        storeTokensInRedis(accessToken, refreshToken, onlineUser);

        // 单设备登录控制
        handleSingleDeviceLogin(user.getUserId(), accessToken);

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
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(formatTokenKey(token));
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
        return redisTemplate.hasKey(formatTokenKey(token));
    }

    /**
     * 校验刷新令牌是否存在于 Redis。
     *
     * @param refreshToken 刷新令牌
     * @return {@code true} 表示有效
     */
    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return redisTemplate.hasKey(formatRefreshTokenKey(refreshToken));
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
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken));
        if (onlineUser == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        String oldAccessToken = (String) redisTemplate.opsForValue().get(StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, onlineUser.getUserId()));

        // 删除旧的访问令牌记录
        if (oldAccessToken != null) {
            redisTemplate.delete(formatTokenKey(oldAccessToken));
        }

        // 生成新访问令牌并存储
        String newAccessToken = IdUtil.fastSimpleUUID();
        storeAccessToken(newAccessToken, onlineUser);

        int accessTtl = securityProperties.getSession().getAccessTokenTimeToLive();
        return AuthenticationToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    /**
     * 注销访问令牌并清理该用户的令牌映射。
     *
     * @param token 访问令牌
     */
    @Override
    public void invalidateToken(String token) {
        OnlineUser onlineUser = resolveOnlineUser(token);
        if (onlineUser != null) {
            Long userId = onlineUser.getUserId();
            // 1. 删除访问令牌相关
            String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId);
            String accessToken = (String) redisTemplate.opsForValue().get(userAccessKey);
            if (accessToken != null) {
                redisTemplate.delete(formatTokenKey(accessToken));
                redisTemplate.delete(userAccessKey);
            }

            // 2. 删除刷新令牌相关
            String userRefreshKey = StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, userId);
            String refreshToken = (String) redisTemplate.opsForValue().get(userRefreshKey);
            if (refreshToken != null) {
                redisTemplate.delete(StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken));
                redisTemplate.delete(userRefreshKey);
            }
        }
    }

    /**
     * 兼容访问令牌和刷新令牌两种输入，解析对应的在线用户。
     *
     * @param token 访问令牌或刷新令牌
     * @return 在线用户，不存在时返回 {@code null}
     */
    private OnlineUser resolveOnlineUser(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(formatTokenKey(token));
        if (onlineUser != null) {
            return onlineUser;
        }
        return (OnlineUser) redisTemplate.opsForValue().get(formatRefreshTokenKey(token));
    }

    /**
     * 持久化访问令牌、刷新令牌以及用户维度映射。
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

        // 用户ID -> 刷新令牌
        setRedisValue(StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, onlineUser.getUserId()),
                refreshToken,
                securityProperties.getSession().getRefreshTokenTimeToLive());
    }

    /**
     * 根据配置执行单设备登录控制。
     *
     * <p>当不允许多端登录时，会删除当前用户旧访问令牌，保证用户仅保留最新会话。</p>
     *
     * @param userId 用户 ID
     * @param accessToken 新访问令牌
     */
    private void handleSingleDeviceLogin(Long userId, String accessToken) {
        Boolean allowMultiLogin = securityProperties.getSession().getRedisToken().getAllowMultiLogin();
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId);
        // 单设备登录控制，删除旧的访问令牌
        if (!allowMultiLogin) {
            String oldAccessToken = (String) redisTemplate.opsForValue().get(userAccessKey);
            if (oldAccessToken != null) {
                redisTemplate.delete(formatTokenKey(oldAccessToken));
            }
        }
        // 存储访问令牌映射（用户ID -> 访问令牌），用于单设备登录控制删除旧的访问令牌和刷新令牌时删除旧令牌
        setRedisValue(userAccessKey, accessToken, securityProperties.getSession().getAccessTokenTimeToLive());
    }

    /**
     * 存储新访问令牌并维护用户到令牌的映射。
     *
     * @param newAccessToken 新访问令牌
     * @param onlineUser 在线用户信息
     */
    private void storeAccessToken(String newAccessToken, OnlineUser onlineUser) {
        setRedisValue(StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, newAccessToken), onlineUser, securityProperties.getSession().getAccessTokenTimeToLive());
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, onlineUser.getUserId());
        setRedisValue(userAccessKey, newAccessToken, securityProperties.getSession().getAccessTokenTimeToLive());
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
}
