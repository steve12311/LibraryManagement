package org.dwtech.common.utils;

import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PrepareMilvusJsonTest {

    private final PrepareMilvusJson prepareMilvusJson = new PrepareMilvusJson();

    @Test
    void shouldPrepareInsertJsonWhenIdIsNumeric() {
        String result = prepareMilvusJson.prepareInsertJson("9787300000001", new float[]{1.0F, 2.0F});

        assertThat(result).contains("\"id\":9787300000001");
        assertThat(result).contains("\"vector\"");
    }

    @Test
    void shouldRejectNonNumericIdWithExplicitBusinessException() {
        Throwable throwable = catchThrowable(() ->
                prepareMilvusJson.prepareInsertJson("ISBN-9787300000001", new float[]{1.0F, 2.0F})
        );

        assertThat(throwable).isInstanceOf(BusinessException.class);
        BusinessException exception = (BusinessException) throwable;
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.PARAMETER_FORMAT_MISMATCH);
        assertThat(exception).hasMessage("AI 向量同步仅支持纯数字 ISBN");
    }
}
