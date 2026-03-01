package org.dwtech.common.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

/**
 * 认证令牌响应对象
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
@Builder
public class AuthenticationToken {

    private String tokenType;

    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private Integer expiresIn;

}
