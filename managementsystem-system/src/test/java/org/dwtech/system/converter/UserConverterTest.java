package org.dwtech.system.converter;

import org.dwtech.system.model.bo.UserBO;
import org.dwtech.system.model.dto.UserExportDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterTest {

    private final UserConverter userConverter = new UserConverterImpl();

    @Test
    void shouldConvertUserToExportDtoWithReadableLabels() {
        UserBO user = new UserBO();
        user.setUsername("reader01");
        user.setNickname("读者一号");
        user.setRoleNames("读者");
        user.setGender(1);
        user.setMobile("13800138000");
        user.setEmail("reader01@test.com");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.of(2026, 4, 20, 10, 30));

        UserExportDTO exportUser = userConverter.toExportDto(user);

        assertThat(exportUser.getUsername()).isEqualTo("reader01");
        assertThat(exportUser.getNickname()).isEqualTo("读者一号");
        assertThat(exportUser.getRoleNames()).isEqualTo("读者");
        assertThat(exportUser.getGenderLabel()).isEqualTo("男");
        assertThat(exportUser.getMobile()).isEqualTo("13800138000");
        assertThat(exportUser.getEmail()).isEqualTo("reader01@test.com");
        assertThat(exportUser.getStatusLabel()).isEqualTo("启用");
        assertThat(exportUser.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 4, 20, 10, 30));
    }

    @Test
    void shouldFormatGenderLabelForKnownAndUnknownValues() {
        assertThat(userConverter.formatGenderLabel(1)).isEqualTo("男");
        assertThat(userConverter.formatGenderLabel(2)).isEqualTo("女");
        assertThat(userConverter.formatGenderLabel(0)).isEqualTo("保密");
        assertThat(userConverter.formatGenderLabel(null)).isEmpty();
        assertThat(userConverter.formatGenderLabel(9)).isEmpty();
    }

    @Test
    void shouldFormatStatusLabelForKnownAndUnknownValues() {
        assertThat(userConverter.formatStatusLabel(1)).isEqualTo("启用");
        assertThat(userConverter.formatStatusLabel(0)).isEqualTo("禁用");
        assertThat(userConverter.formatStatusLabel(2)).isEqualTo("禁用");
        assertThat(userConverter.formatStatusLabel(null)).isEmpty();
    }
}
