package org.dwtech.framework.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.model.entity.OperLogPO;
import org.dwtech.system.service.OperLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperLogAspectTest {

    @Mock
    private OperLogService operLogService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldRecordSuccessfulOperLogWithResolvedBizId() throws Throwable {
        OperLogAspect aspect = new OperLogAspect(operLogService);
        Method method = DummyOperationHolder.class.getDeclaredMethod("assignMenus", Long.class);
        prepareRequest("/api/v1/roles/1001/menus");
        authenticateCurrentUser(99L, "admin");
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{1001L});
        when(proceedingJoinPoint.proceed()).thenReturn(Result.success());

        Object result = aspect.recordOperLog(proceedingJoinPoint, method.getAnnotation(OperLog.class));

        assertThat(result).isInstanceOf(Result.class);
        ArgumentCaptor<OperLogPO> captor = ArgumentCaptor.forClass(OperLogPO.class);
        verify(operLogService).saveQuietly(captor.capture());
        OperLogPO operLog = captor.getValue();
        assertThat(operLog.getModule()).isEqualTo("角色管理");
        assertThat(operLog.getAction()).isEqualTo("分配菜单");
        assertThat(operLog.getBizResourceId()).isEqualTo("1001");
        assertThat(operLog.getOperatorUserId()).isEqualTo(99L);
        assertThat(operLog.getOperatorUsername()).isEqualTo("admin");
        assertThat(operLog.getRequestMethod()).isEqualTo("PUT");
        assertThat(operLog.getRequestPath()).isEqualTo("/api/v1/roles/1001/menus");
        assertThat(operLog.getClientIp()).isEqualTo("10.0.0.8");
        assertThat(operLog.getSuccess()).isEqualTo(1);
        assertThat(operLog.getResultCode()).isEqualTo(ResultCode.SUCCESS.getCode());
    }

    @Test
    void shouldRecordFailedOperLogWhenBusinessExceptionThrown() throws Throwable {
        OperLogAspect aspect = new OperLogAspect(operLogService);
        Method method = DummyOperationHolder.class.getDeclaredMethod("deleteUser", Long.class);
        prepareRequest("/api/v1/users/2001");
        authenticateCurrentUser(88L, "librarian");
        BusinessException exception = new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "用户不存在");
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{2001L});
        when(proceedingJoinPoint.proceed()).thenThrow(exception);

        BusinessException actual = assertThrows(
                BusinessException.class,
                () -> aspect.recordOperLog(proceedingJoinPoint, method.getAnnotation(OperLog.class))
        );

        assertThat(actual).isEqualTo(exception);
        ArgumentCaptor<OperLogPO> captor = ArgumentCaptor.forClass(OperLogPO.class);
        verify(operLogService).saveQuietly(captor.capture());
        OperLogPO operLog = captor.getValue();
        assertThat(operLog.getModule()).isEqualTo("用户管理");
        assertThat(operLog.getAction()).isEqualTo("删除用户");
        assertThat(operLog.getBizResourceId()).isEqualTo("2001");
        assertThat(operLog.getSuccess()).isEqualTo(0);
        assertThat(operLog.getResultCode()).isEqualTo(ResultCode.USER_RESOURCE_NOT_FOUND.getCode());
        assertThat(operLog.getErrorSummary()).isEqualTo("用户不存在");
    }

    private void prepareRequest(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        request.setRequestURI(requestUri);
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void authenticateCurrentUser(Long userId, String username) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(userId);
        userDetails.setUsername(username);
        userDetails.setAuthorities(Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    static class DummyOperationHolder {

        @OperLog(module = "角色管理", action = "分配菜单", bizId = "#p0")
        void assignMenus(Long roleId) {
        }

        @OperLog(module = "用户管理", action = "删除用户", bizId = "#p0")
        void deleteUser(Long userId) {
        }
    }
}
