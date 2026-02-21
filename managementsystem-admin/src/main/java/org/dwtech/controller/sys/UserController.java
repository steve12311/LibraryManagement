package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.PasswordUpdateForm;
import org.dwtech.common.core.entity.form.UserForm;
import org.dwtech.common.core.entity.query.UserPageQuery;
import org.dwtech.common.core.entity.vo.CurrentUserVO;
import org.dwtech.common.core.entity.vo.UserPageVO;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/page")
    public PageResult<UserPageVO> getUserPage(@Valid UserPageQuery queryParams) {
        IPage<UserPageVO> result = userService.getUserPage(queryParams);
        return PageResult.success(result);
    }

    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    public Result<?> saveUser(@RequestBody @Valid UserForm formData) {
        boolean result = userService.saveUser(formData);
        return Result.judge(result);
    }

    @GetMapping("/{userId}/form")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    public Result<UserForm> getUserForm(@PathVariable("userId") Long userId) {
        UserForm formData = userService.getUserFormData(userId);
        return Result.success(formData);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    public Result<?> updateUser(@PathVariable("userId") Long userId, @RequestBody @Valid UserForm formData) {
        boolean result = userService.updateUser(userId, formData);
        return Result.judge(result);
    }

    @DeleteMapping("/{userIds}")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    public Result<?> deleteUsers(@PathVariable("userIds") List<Long> userIds) {
        boolean result = userService.deleteUsers(userIds);
        return Result.judge(result);
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    public Result<?> updateUserStatus(@PathVariable("userId") Long userId, @RequestParam("status") Integer status) {
        boolean result = userService.updateUserStatus(userId, status);
        return Result.judge(result);
    }

    @GetMapping("/me")
    public Result<CurrentUserVO> getCurrentUser() {
        CurrentUserVO currentUser = userService.getCurrentUserInfo();
        return Result.success(currentUser);
    }

    @PutMapping("/{userId}/password/reset")
    @PreAuthorize("@ss.hasPerm('sys:user:reset-password')")
    public Result<?> resetPassword(@PathVariable("userId") Long userId, @RequestParam("password") String password) {
        boolean result = userService.resetPassword(userId, password);
        return Result.judge(result);
    }

    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody PasswordUpdateForm formData) {
        Long userId = SecurityUtils.getUserId();
        boolean result = userService.changePassword(userId, formData);
        return Result.judge(result);
    }

    @GetMapping("/options")
    public Result<List<Option<String>>> listUserOptions() {
        List<Option<String>> list = userService.listUserOptions();
        return Result.success(list);
    }
}
