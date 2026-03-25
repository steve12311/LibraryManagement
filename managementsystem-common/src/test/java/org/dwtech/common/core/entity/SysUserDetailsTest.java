package org.dwtech.common.core.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserDetailsTest {

    @Test
    void shouldReturnFalseWhenUserStatusIsDisabled() {
        UserAuthCredentials userAuthCredentials = new UserAuthCredentials();
        userAuthCredentials.setUserId(1L);
        userAuthCredentials.setUsername("alice");
        userAuthCredentials.setPassword("secret");
        userAuthCredentials.setStatus(0);

        SysUserDetails userDetails = new SysUserDetails(userAuthCredentials);

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void shouldReturnTrueWhenUserStatusIsEnabled() {
        UserAuthCredentials userAuthCredentials = new UserAuthCredentials();
        userAuthCredentials.setUserId(1L);
        userAuthCredentials.setUsername("alice");
        userAuthCredentials.setPassword("secret");
        userAuthCredentials.setStatus(1);

        SysUserDetails userDetails = new SysUserDetails(userAuthCredentials);

        assertThat(userDetails.isEnabled()).isTrue();
    }
}
