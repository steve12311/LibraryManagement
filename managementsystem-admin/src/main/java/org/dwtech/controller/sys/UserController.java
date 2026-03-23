package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.query.MyBorrowPageQuery;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.MyBorrowPageVO;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.vo.UserPageVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
     * 用途：获取 user page 信息。
     * 
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public PageResult<UserPageVO> getUserPage(@Valid UserPageQuery queryParams) {
        IPage<UserPageVO> result = userService.getUserPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 用途：保存 user。
     * 
     * @param formData form data
     * @return 返回结果
     */
    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    public Result<?> saveUser(@RequestBody @Valid UserForm formData) {
        boolean result = userService.saveUser(formData);
        return Result.judge(result);
    }

    /**
     * 用途：获取 user form 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    @GetMapping("/{userId}/form")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    public Result<UserForm> getUserForm(@PathVariable("userId") Long userId) {
        UserForm formData = userService.getUserFormData(userId);
        return Result.success(formData);
    }

    /**
     * 用途：更新 user。
     * 
     * @param userId user ID
     * @param formData form data
     * @return 返回结果
     */
    @PutMapping("/{userId}")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @RepeatSubmit
    public Result<?> updateUser(@PathVariable("userId") Long userId, @RequestBody @Valid UserForm formData) {
        formData.setId(userId);
        boolean result = userService.updateUser(userId, formData);
        return Result.judge(result);
    }

    /**
     * 用途：删除 users。
     * 
     * @param userIds user ID 列表
     * @return 返回结果
     */
    @DeleteMapping("/{userIds}")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    @RepeatSubmit
    public Result<?> deleteUsers(@PathVariable("userIds") List<Long> userIds) {
        boolean result = userService.deleteUsers(userIds);
        return Result.judge(result);
    }

    /**
     * 用途：更新 user status。
     * 
     * @param userId user ID
     * @param status status
     * @return 返回结果
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    @RepeatSubmit
    public Result<?> updateUserStatus(@PathVariable("userId") Long userId, @RequestParam("status") Integer status) {
        boolean result = userService.updateUserStatus(userId, status);
        return Result.judge(result);
    }

    /**
     * 用途：获取 current user 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<CurrentUserVO> getCurrentUser() {
        CurrentUserVO currentUser = userService.getCurrentUserInfo();
        return Result.success(currentUser);
    }

    /**
     * 用途：获取当前登录用户借阅订单分页信息。
     *
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping("/me/borrows/page")
    @PreAuthorize("isAuthenticated()")
    public PageResult<MyBorrowPageVO> getCurrentUserBorrowPage(@Valid MyBorrowPageQuery queryParams) {
        Long userId = SecurityUtils.getUserId();
        IPage<MyBorrowPageVO> result = borrowService.getCurrentUserBorrowPage(userId, queryParams);
        return PageResult.success(result);
    }

    /**
     * 用途：重置 password。
     * 
     * @param userId user ID
     * @param password password
     * @return 返回结果
     */
    @PutMapping("/{userId}/password/reset")
    @PreAuthorize("@ss.hasPerm('sys:user:reset-password')")
    @RepeatSubmit
    public Result<?> resetPassword(@PathVariable("userId") Long userId, @RequestParam("password") String password) {
        boolean result = userService.resetPassword(userId, password);
        return Result.judge(result);
    }

    /**
     * 用途：更新 password。
     * 
     * @param formData form data
     * @return 返回结果
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
     * 用途：查询 user options 列表。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public Result<List<Option<String>>> listUserOptions() {
        List<Option<String>> list = userService.listUserOptions();
        return Result.success(list);
    }

    /**
     * 用途：获取 user profile 信息。
     * 
     * 入参：无。
     * @return 返回结果
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
     * 用途：更新 user profile。
     * 
     * @param formData form data
     * @return 返回结果
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
