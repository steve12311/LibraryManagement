package org.dwtech.common.token;


import org.dwtech.common.core.entity.AuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 *  Token 管理器
 *  <p>
 *  用于生成、解析、校验、刷新 Token
 *
 * @author steve12311
 * @since 2025-11-18
 */
public interface TokenManager {

    /**
     * 用途：生成 token。
     * 
     * 生成认证 Token
     *
     * @param authentication 用户认证信息
     * @return 认证 Token 响应
     */
    AuthenticationToken generateToken(Authentication authentication);

    /**
     * 用途：解析 token。
     * 
     * 解析 Token 获取认证信息
     *
     * @param token  Token
     * @return 用户认证信息
     */
    Authentication parseToken(String token);

    /**
     * 用途：校验 token。
     * 
     * 校验 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 用途：校验 refresh token。
     * 
     * 校验 刷新 Token 是否有效
     *
     * @param refreshToken JWT Token
     * @return 是否有效
     */
    boolean validateRefreshToken(String refreshToken);

    /**
     * 用途：刷新 token。
     * 
     *  刷新 Token
     *
     * @param token 刷新令牌
     * @return 认证 Token 响应
     */
    AuthenticationToken refreshToken(String token);

    /**
     * 用途：执行 invalidate token 操作。
     * 
     * 令 Token 失效
     *
     * @param token Token
     * 返回：无。
     */
    default void invalidateToken(String token) {
        // 默认实现可以是空的，或者抛出不支持的操作异常
        // throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * 按用户维度使其现有会话全部失效。
     *
     * @param userId 用户 ID
     */
    default void invalidateUserSessions(Long userId) {
        // 默认实现可以是空的，具体由各令牌实现决定
    }


}
