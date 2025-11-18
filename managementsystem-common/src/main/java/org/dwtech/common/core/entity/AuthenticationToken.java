package org.dwtech.common.core.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 认证令牌响应对象
 */
@Data
@Builder
public class AuthenticationToken {

    private String tokenType;

    private String accessToken;

    private String refreshToken;

    private Integer expiresIn;

}
