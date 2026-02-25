package org.dwtech.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.dwtech.common.plugin.MyDataPermissionHandler;
import org.dwtech.common.plugin.MyMetaObjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mybatis-plus 配置类
 *
 * @author steve12311
* @since 2025-11-18
 */
/**
 * MybatisConfig
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Configuration
@EnableTransactionManagement
public class MybatisConfig {

    /**
     * 用途：执行 mybatis plus interceptor 操作。
     * 
     * 分页插件和数据权限插件
     * 
     * 入参：无。
     * @return 返回结果
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //数据权限
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MyDataPermissionHandler()));
        //分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 用途：执行 global config 操作。
     * 
     * 自动填充数据库创建人、创建时间、更新人、更新时间
     * 
     * 入参：无。
     * @return 返回结果
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new MyMetaObjectHandler());
        return globalConfig;
    }

}
