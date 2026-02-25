package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
/**
 * PublishPageVO
 *
 * @author steve12311
 * @since 2026-02-22
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class PublishPageVO extends BaseVO {
    private Long publishId;
    private String publishName;
    private String address;
    private String addressCode;
    private String phonenumber;
    private Date createTime;
}
