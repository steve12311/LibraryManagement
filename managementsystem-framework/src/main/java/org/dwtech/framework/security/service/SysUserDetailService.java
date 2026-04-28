package org.dwtech.framework.security.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.common.utils.IPUtils;
import org.dwtech.common.utils.ServletUtils;
import org.dwtech.system.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
/**
 * SysUserDetailService
 * Spring Security 用户详情加载服务。实现 UserDetailsService 接口，
 * 根据用户名从用户服务加载认证凭据，并在认证失败时记录安全日志。
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserDetailService implements UserDetailsService {
    private final UserService userService;

    /**
     * 根据用户名加载用户详情。流程：从用户服务获取认证凭据 →
     * 若未找到则抛出 UsernameNotFoundException → 构造 SysUserDetails 返回。
     * 认证失败时记录客户端 IP 和安全日志。
     *
     * @param username 用户名
     * @return Spring Security 用户详情
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = StrUtil.blankToDefault(StrUtil.trim(username), "unknown");
        String clientIp = resolveClientIp();
        try {
            UserAuthCredentials userAuthCredentials = userService.getAuthCredentialsByUsername(username);
            if (userAuthCredentials == null) {
                throw new UsernameNotFoundException(username);
            }
            return new SysUserDetails(userAuthCredentials);
        } catch (UsernameNotFoundException e) {
            log.warn("认证失败, action=load_user, username={}, clientIp={}, result=username_not_found",
                    normalizedUsername, clientIp);
            throw e;
        } catch (Exception e) {
            log.error("认证异常, action=load_user, username={}, clientIp={}, exceptionType={}",
                    normalizedUsername, clientIp, e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * 获取当前认证请求的客户端 IP。
     *
     * @return 客户端 IP，不可获取时返回 unknown
     */
    private String resolveClientIp() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return "unknown";
        }
        return StrUtil.blankToDefault(IPUtils.getIpAddr(ServletUtils.getRequest()), "unknown");
    }
}
