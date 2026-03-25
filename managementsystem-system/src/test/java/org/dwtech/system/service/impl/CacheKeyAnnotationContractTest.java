package org.dwtech.system.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CacheKeyAnnotationContractTest {

    @Test
    void menuOptionsShouldUsePositionalParameterKey() throws NoSuchMethodException {
        Method method = MenuServiceImpl.class.getMethod("listMenuOptions", boolean.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.key()).isEqualTo("'options:' + #p0");
    }

    @Test
    void roleMenuIdsShouldUsePositionalParameterKey() throws NoSuchMethodException {
        Method method = RoleServiceImpl.class.getMethod("getRoleMenuIds", Long.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.key()).isEqualTo("'menuIds:' + #p0");
    }
}
