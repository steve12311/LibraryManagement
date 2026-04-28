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
     * 根据用户认证信息生成认证 Token。
     *
     * @param authentication 用户认证信息
     * @return 认证 Token 响应
     */
    AuthenticationToken generateToken(Authentication authentication);

    /**
     * 解析 Token 字符串获取用户认证信息。
     *
     * @param token Token 字符串
     * @return 用户认证信息
     */
    Authentication parseToken(String token);

    /**
     * 校验 Token 是否有效。
     *
     * @param token JWT Token
     * @return true 表示有效，false 表示无效
     */
    boolean validateToken(String token);

    /**
     * 校验刷新 Token 是否有效。
     *
     * @param refreshToken 刷新 Token
     * @return true 表示有效，false 表示无效
     */
    boolean validateRefreshToken(String refreshToken);

    /**
     * 刷新 Token，返回新的认证 Token 响应。
     *
     * @param token 刷新令牌
     * @return 新的认证 Token 响应
     */
    AuthenticationToken refreshToken(String token);

    /**
     * 令指定 Token 失效（登出时调用）。
     *
     * @param token 待失效的 Token
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
