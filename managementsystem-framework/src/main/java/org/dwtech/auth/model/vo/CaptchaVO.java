package org.dwtech.auth.model.vo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;
/**
 * CaptchaVO
 *
 * @author steve12311
 * @since 2025-11-18
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class CaptchaVO extends BaseVO {
    private String captchaKey;
    private String captchaBase64;
}
