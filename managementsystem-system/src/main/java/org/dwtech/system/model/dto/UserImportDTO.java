package org.dwtech.system.model.dto;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 用户导入数据传输对象
 *
 * @author steve12311
 * @since 2026-04-20
 */
@Data
@ColumnWidth(20)
public class UserImportDTO {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("性别")
    private String genderLabel;

    @ExcelProperty("手机号码")
    private String mobile;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("角色")
    private String roleNames;
}
