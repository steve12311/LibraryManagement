package org.dwtech.common.plugin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.dwtech.common.annontation.DataPermission;
import org.dwtech.common.enmus.DataScopeEnum;
import org.dwtech.common.enmus.IBaseEnum;
import org.dwtech.common.utils.SecurityUtils;

import java.lang.reflect.Method;
/**
 * MyBatis-Plus 数据权限处理器 — 行级数据隔离核心
 * <p>
 * 当 Mapper 方法标注 {@link DataPermission} 时，此处理器根据当前登录用户的数据权限范围
 * 自动向原始 SQL 追加 WHERE 条件，实现：
 * <ul>
 *   <li>{@code ALL} — 不追加条件，可查看全部数据</li>
 *   <li>{@code DEPT} — 仅查看本部门数据</li>
 *   <li>{@code DEPT_AND_CHILD} — 查看本部门及所有子部门数据（通过 tree_path 递归匹配）</li>
 *   <li>{@code SELF} — 仅查看本人数据</li>
 * </ul>
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Slf4j
public class MyDataPermissionHandler implements DataPermissionHandler {

    /**
     * MyBatis-Plus 拦截器回调，在 SQL 执行前动态拼接数据权限条件。
     * <p>
     * 通过 {@code mappedStatementId} 反查当前执行的 Mapper 方法，读取其 {@link DataPermission} 注解配置，
     * 再根据用户数据权限级别生成对应的 WHERE 片段，与原始 WHERE 以 AND 连接。
     *
     * @param where              原始 WHERE 条件表达式
     * @param mappedStatementId  Mapper 全限定方法名（如 {@code org.dwtech.system.mapper.BookMapper.selectPage}）
     * @return 追加数据权限条件后的完整 WHERE 表达式
     */
    @Override
    @SneakyThrows
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 未登录用户或超级管理员不追加数据权限条件
        if (SecurityUtils.getUserId() == null || SecurityUtils.isRoot()) {
            return where;
        }
        Integer dataScope = SecurityUtils.getDataScope();
        DataScopeEnum dataScopeEnum = IBaseEnum.getEnumByValue(dataScope, DataScopeEnum.class);
        if (DataScopeEnum.ALL.equals(dataScopeEnum)) {
            return where;
        }
        // 反射获取当前执行的 Mapper 方法，读取 @DataPermission 注解配置
        Class<?> clazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(StringPool.DOT)));
        String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(StringPool.DOT) + 1);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                DataPermission annotation = method.getAnnotation(DataPermission.class);
                if (annotation == null) {
                    return where;
                }
                return dataScopeFilter(
                        annotation.deptAlias(), annotation.deptIdColumnName(),
                        annotation.userAlias(), annotation.userIdColumnName(),
                        dataScopeEnum, where);
            }
        }
        return where;
    }

    /**
     * 根据数据权限级别构建具体的 SQL 过滤条件。
     * <ul>
     *   <li>{@code DEPT}：{@code dept_id = 当前用户部门ID}</li>
     *   <li>{@code SELF}：{@code create_by = 当前用户ID}</li>
     *   <li>{@code DEPT_AND_CHILD}：通过 {@code FIND_IN_SET} 递归匹配部门树</li>
     * </ul>
     *
     * @param deptAlias        SQL 中部门表别名
     * @param deptIdColumnName 部门 ID 列
     * @param userAlias        SQL 中用户表别名
     * @param userIdColumnName 用户 ID（创建人）列
     * @param dataScopeEnum    当前用户数据权限级别
     * @param where            原始 WHERE 条件
     * @return 拼接后的 WHERE 条件表达式
     */
    @SneakyThrows
    public static Expression dataScopeFilter(String deptAlias, String deptIdColumnName,
                                              String userAlias, String userIdColumnName,
                                              DataScopeEnum dataScopeEnum, Expression where) {

        String deptColumnName = StrUtil.isNotBlank(deptAlias)
                ? (deptAlias + StringPool.DOT + deptIdColumnName) : deptIdColumnName;
        String userColumnName = StrUtil.isNotBlank(userAlias)
                ? (userAlias + StringPool.DOT + userIdColumnName) : userIdColumnName;

        Long deptId, userId;
        String appendSqlStr;
        switch (dataScopeEnum) {
            case ALL:
                return where;
            case DEPT:
                deptId = SecurityUtils.getDeptId();
                appendSqlStr = deptColumnName + StringPool.EQUALS + deptId;
                break;
            case SELF:
                userId = SecurityUtils.getUserId();
                appendSqlStr = userColumnName + StringPool.EQUALS + userId;
                break;
            // DEPT_AND_CHILD：查询本部门及所有子部门数据
            default:
                deptId = SecurityUtils.getDeptId();
                appendSqlStr = deptColumnName
                        + " IN ( SELECT id FROM sys_dept WHERE id = " + deptId
                        + " OR FIND_IN_SET( " + deptId + " , tree_path ) )";
                break;
        }

        if (StrUtil.isBlank(appendSqlStr)) {
            return where;
        }

        Expression appendExpression = CCJSqlParserUtil.parseCondExpression(appendSqlStr);

        if (where == null) {
            return appendExpression;
        }

        return new AndExpression(where, appendExpression);
    }
}
