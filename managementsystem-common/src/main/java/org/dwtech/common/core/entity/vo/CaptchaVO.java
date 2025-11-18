package org.dwtech.common.core.entity.vo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class CaptchaVO extends BaseVO {
    private String captchaKey;
    private String captchaBase64;
}
