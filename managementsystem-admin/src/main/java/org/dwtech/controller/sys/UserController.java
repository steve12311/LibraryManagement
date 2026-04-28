package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.idev.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import org.dwtech.common.annontation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.dto.UserExportDTO;
import org.dwtech.system.model.dto.UserImportDTO;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.query.MyBorrowPageQuery;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.MyBorrowPageVO;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.vo.UserImportResultVO;
import org.dwtech.system.model.vo.UserPageVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
/**
 * UserController
 *
 * @author steve12311
 * @since 2025-11-18
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final BorrowService borrowService;

    /**
     * 分页查询用户列表。
     * 支持按用户名、手机号、状态、部门等条件筛选。
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public PageResult<UserPageVO> getUserPage(@Valid UserPageQuery queryParams) {
        IPage<UserPageVO> result = userService.getUserPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 新增用户。
     * 接收用户表单数据，校验用户名唯一性，密码加密后写入数据库并记录操作日志。
     */
    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    @OperLog(module = "用户管理", action = "新增用户", bizId = "#p0.username")
    public Result<?> saveUser(@RequestBody @Valid UserForm formData) {
        boolean result = userService.saveUser(formData);
        return Result.judge(result);
    }

    /**
     * 下载用户导入模板 Excel 文件。
     * 模板包含用户导入所需的字段列（用户名、手机号、邮箱等），供用户填写后批量导入。
     */
    @GetMapping("/template")
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    public void downloadUserTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("用户导入模板.xlsx", StandardCharsets.UTF_8)
        );
        EasyExcel.write(response.getOutputStream(), UserImportDTO.class)
                .sheet("用户导入模板")
                .doWrite(List.of());
    }

    /**
     * 批量导入用户。
     * 通过上传 Excel 文件批量创建用户，逐行校验数据合法性，返回导入结果（成功数/失败数/失败详情）。
     *
     * @param file 用户导入 Excel 文件
     */
    @PostMapping("/import")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    @OperLog(module = "用户管理", action = "导入用户")
    public Result<UserImportResultVO> importUsers(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY, "导入文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            UserImportResultVO result = userService.importUsers(inputStream);
            return Result.success(result);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_EXCEPTION, "读取导入文件失败");
        }
    }

    /**
     * 导出用户列表为 Excel 文件。
     * 根据查询条件筛选用户数据，使用 EasyExcel 生成用户列表 xlsx 并写入响应流供下载。
     */
    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public void exportUsers(@Valid UserPageQuery queryParams, HttpServletResponse response) throws IOException {
        List<UserExportDTO> exportUsers = userService.listExportUsers(queryParams);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("用户列表.xlsx", StandardCharsets.UTF_8)
        );
        EasyExcel.write(response.getOutputStream(), UserExportDTO.class)
                .sheet("用户列表")
                .doWrite(exportUsers);
    }

    /**
     * 根据 ID 获取用户表单数据，用于编辑时回显。
     *
     * @param userId 用户 ID
     */
    @GetMapping("/{userId}/form")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    public Result<UserForm> getUserForm(@PathVariable("userId") Long userId) {
        UserForm formData = userService.getUserFormData(userId);
        return Result.success(formData);
    }

    /**
     * 修改用户信息。
     * 根据路径 ID 和表单数据更新用户基本属性，校验用户名唯一性并记录操作日志。
     *
     * @param userId   用户 ID
     * @param formData 用户表单数据
     */
    @PutMapping("/{userId}")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @RepeatSubmit
    @OperLog(module = "用户管理", action = "修改用户", bizId = "#p0")
    public Result<?> updateUser(@PathVariable("userId") Long userId, @RequestBody @Valid UserForm formData) {
        formData.setId(userId);
        boolean result = userService.updateUser(userId, formData);
        return Result.judge(result);
    }

    /**
     * 批量删除用户。
     * 根据用户 ID 列表删除用户记录，不能删除自身。
     *
     * @param userIds 用户 ID 列表
     */
    @DeleteMapping("/{userIds}")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    @RepeatSubmit
    @OperLog(module = "用户管理", action = "删除用户", bizId = "#p0")
    public Result<?> deleteUsers(@PathVariable("userIds") List<Long> userIds) {
        boolean result = userService.deleteUsers(userIds);
        return Result.judge(result);
    }

    /**
     * 修改用户启用/禁用状态。
     *
     * @param userId 用户 ID
     * @param status 目标状态：1 启用，0 禁用
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @RepeatSubmit
    @OperLog(module = "用户管理", action = "修改用户状态", bizId = "#p0")
    public Result<?> updateUserStatus(@PathVariable("userId") Long userId, @RequestParam("status") Integer status) {
        boolean result = userService.updateUserStatus(userId, status);
        return Result.judge(result);
    }

    /**
     * 获取当前登录用户信息。
     * 包含用户基本信息、角色和权限集合，用于前端用户状态管理和权限判断。
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<CurrentUserVO> getCurrentUser() {
        CurrentUserVO currentUser = userService.getCurrentUserInfo();
        return Result.success(currentUser);
    }

    /**
     * 分页查询当前登录用户的借阅订单。
     * 仅返回该用户自己的借阅记录，支持按借阅状态、日期等条件筛选。
     */
    @GetMapping("/me/borrows/page")
    @PreAuthorize("isAuthenticated()")
    public PageResult<MyBorrowPageVO> getCurrentUserBorrowPage(@Valid MyBorrowPageQuery queryParams) {
        Long userId = SecurityUtils.getUserId();
        IPage<MyBorrowPageVO> result = borrowService.getCurrentUserBorrowPage(userId, queryParams);
        return PageResult.success(result);
    }

    /**
     * 管理员重置指定用户的密码。
     * 将用户密码更新为指定的新值（明文），由服务端进行加密处理后存储。
     *
     * @param userId   用户 ID
     * @param password 新密码明文
     */
    @PutMapping("/{userId}/password/reset")
    @PreAuthorize("@ss.hasPerm('sys:user:reset-password')")
    @RepeatSubmit
    @OperLog(module = "用户管理", action = "重置用户密码", bizId = "#p0")
    public Result<?> resetPassword(@PathVariable("userId") Long userId, @RequestParam("password") String password) {
        boolean result = userService.resetPassword(userId, password);
        return Result.judge(result);
    }

    /**
     * 当前登录用户修改自己的密码。
     * 校验旧密码是否正确，新旧密码不能相同，更新后强制退出重新登录。
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @RepeatSubmit
    public Result<?> updatePassword(@RequestBody PasswordUpdateForm formData) {
        Long userId = SecurityUtils.getUserId();
        boolean result = userService.changePassword(userId, formData);
        return Result.judge(result);
    }

    /**
     * 查询用户选项列表。
     * 返回用户的 ID 和名称键值对，用于前端下拉选择器。
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public Result<List<Option<String>>> listUserOptions() {
        List<Option<String>> list = userService.listUserOptions();
        return Result.success(list);
    }

    /**
     * 获取当前登录用户的个人资料。
     * 返回用户的详细信息，用于个人中心页面展示。
     */
    @Operation(summary = "获取个人中心用户信息")
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<UserProfileVO> getUserProfile() {
        Long userId = SecurityUtils.getUserId();
        UserProfileVO userProfile = userService.getUserProfile(userId);
        return Result.success(userProfile);
    }

    /**
     * 当前登录用户修改个人资料。
     * 支持更新昵称、手机号、邮箱、头像等个人信息。
     */
    @Operation(summary = "个人中心修改用户信息")
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @RepeatSubmit
    public Result<?> updateUserProfile(@RequestBody UserProfileForm formData) {
        boolean result = userService.updateUserProfile(formData);
        return Result.judge(result);
    }
}
