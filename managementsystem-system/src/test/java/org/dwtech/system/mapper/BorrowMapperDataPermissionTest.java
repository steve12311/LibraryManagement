package org.dwtech.system.mapper;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.DataScopeEnum;
import org.dwtech.common.plugin.MyDataPermissionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BorrowMapperDataPermissionTest {

    private final MyDataPermissionHandler dataPermissionHandler = new MyDataPermissionHandler();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldKeepOriginalConditionWhenBorrowMapperHasNoDataPermission() throws Exception {
        setCurrentUser(1001L, 2001L, DataScopeEnum.SELF.getValue());
        Expression where = CCJSqlParserUtil.parseCondExpression("bw.isbn = '9787300000001'");

        Expression actual = dataPermissionHandler.getSqlSegment(
                where,
                "org.dwtech.system.mapper.BorrowMapper.getBorrowPage"
        );

        assertThat(actual.toString()).isEqualTo(where.toString());
        assertThat(actual.toString()).doesNotContain("bw.user_id");
        assertThat(actual.toString()).doesNotContain("u.dept_id");
    }

    private void setCurrentUser(Long userId, Long deptId, Integer dataScope) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(userId);
        userDetails.setDeptId(deptId);
        userDetails.setDataScope(dataScope);
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }
}
