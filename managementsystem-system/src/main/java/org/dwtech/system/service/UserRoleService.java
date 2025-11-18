package org.dwtech.system.service;

import java.util.List;

public interface UserRoleService {
    void saveUserRoles(Long id, List<Long> roleIds);
}
