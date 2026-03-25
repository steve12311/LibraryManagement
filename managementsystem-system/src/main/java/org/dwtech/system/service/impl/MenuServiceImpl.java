package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.dwtech.common.constant.SystemConstants;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.MenuForm;
import org.dwtech.system.model.form.PermitForm;
import org.dwtech.system.model.entity.MenuPO;
import org.dwtech.system.model.query.MenuQuery;
import org.dwtech.system.model.vo.MenuVO;
import org.dwtech.system.model.vo.RouteVO;
import org.dwtech.common.enmus.MenuTypeEnum;
import org.dwtech.common.enmus.StatusEnum;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.converter.MenuConverter;
import org.dwtech.system.mapper.MenuMapper;
import org.dwtech.system.service.MenuService;
import org.dwtech.system.service.RoleMenuService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MenuServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuPO> implements MenuService {
    private final MenuConverter menuConverter;
    private final RoleMenuService roleMenuService;

    /**
     * 查询菜单列表并构建树形结构。
     *
     * @param queryParams 菜单查询参数（关键字、状态）
     * @return 菜单树列表
     */
    @Override
    public List<MenuVO> listMenus(MenuQuery queryParams) {
        List<MenuPO> menus = this.list(new LambdaQueryWrapper<MenuPO>()
                .in(MenuPO::getType, MenuTypeEnum.CATALOG.getValue(), MenuTypeEnum.MENU.getValue())
                .like(StrUtil.isNotBlank(queryParams.getKeywords()), MenuPO::getName, queryParams.getKeywords())
                .eq(ObjectUtil.isNotNull(queryParams.getStatus()), MenuPO::getVisible, queryParams.getStatus())
                .orderByAsc(MenuPO::getSort)
        );
        // 获取所有菜单ID
        Set<Long> menuIds = menus.stream()
                .map(MenuPO::getId)
                .collect(Collectors.toSet());

        // 获取所有父级ID
        Set<Long> parentIds = menus.stream()
                .map(MenuPO::getParentId)
                .collect(Collectors.toSet());

        // 获取根节点ID（递归的起点），即父节点ID中不包含在部门ID中的节点，注意这里不能拿顶级菜单 O 作为根节点，因为菜单筛选的时候 O 会被过滤掉
        List<Long> rootIds = parentIds.stream()
                .filter(id -> !menuIds.contains(id))
                .toList();

        Map<Long, List<MenuPO>> menuChildrenMap = groupMenusByParentId(menus);
        // 使用递归函数来构建菜单树
        return rootIds.stream()
                .flatMap(rootId -> buildMenuTree(rootId, menuChildrenMap).stream())
                .collect(Collectors.toList());
    }

    /**
     * 查询菜单下拉选项。
     *
     * @param onlyParent 是否仅包含目录节点
     * @return 菜单下拉树
     */
    @Override
    @Cacheable(cacheNames = "menu", key = "'options:' + #p0")
    public List<Option<Long>> listMenuOptions(boolean onlyParent) {
        List<MenuPO> menuList = this.list(new LambdaQueryWrapper<MenuPO>()
                .in(onlyParent, MenuPO::getType, MenuTypeEnum.CATALOG.getValue(), MenuTypeEnum.MENU.getValue())
                .orderByAsc(MenuPO::getSort)
        );
        Map<Long, List<MenuPO>> menuChildrenMap = groupMenusByParentId(menuList);
        return buildMenuOptions(SystemConstants.ROOT_NODE_ID, menuChildrenMap);
    }

    /**
     * 查询当前用户可访问的前端路由树。
     *
     * <p>超级管理员返回全部非按钮菜单，普通用户按角色权限返回。</p>
     *
     * @return 当前用户路由列表
     */
    @Override
    @Cacheable(cacheNames = "menu", key = "'routes:' + T(org.dwtech.common.utils.SecurityUtils).getUserId()")
    public List<RouteVO> listCurrentUserRoutes() {
        Set<String> roleCodes = SecurityUtils.getRoles();

        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptyList();
        }

        List<MenuPO> menuList;
        if (SecurityUtils.isRoot()) {
            // 超级管理员获取所有菜单
            menuList = this.list(new LambdaQueryWrapper<MenuPO>()
                    .ne(MenuPO::getType, MenuTypeEnum.BUTTON.getValue())
                    .orderByAsc(MenuPO::getSort)
            );
        } else {
            menuList = this.baseMapper.getMenusByRoleCodes(roleCodes);
        }
        Map<Long, List<MenuPO>> menuChildrenMap = groupMenusByParentId(menuList);
        return buildRoutes(SystemConstants.ROOT_NODE_ID, menuChildrenMap);
    }

    /**
     * 获取菜单编辑表单详情。
     *
     * @param id 菜单 ID
     * @return 菜单表单对象，包含按钮权限配置
     */
    @Override
    public MenuForm getMenuForm(Long id) {
        MenuPO entity = this.getById(id);
        Assert.isTrue(entity != null, "菜单不存在");
        List<MenuPO> permission = this.list(new LambdaQueryWrapper<MenuPO>()
                .eq(MenuPO::getType, MenuTypeEnum.BUTTON)
                .eq(MenuPO::getParentId, id)
        );
        MenuForm formData = menuConverter.toForm(entity);
        List<PermitForm> permit = menuConverter.toPermitForms(permission);
        formData.setPerms(permit);
//        // 路由参数字符串 {"id":"1","name":"张三"} 转换为 [{key:"id", value:"1"}, {key:"name", value:"张三"}]
//        String params = entity.getParams();
//        if (StrUtil.isNotBlank(params)) {
//            ObjectMapper objectMapper = new ObjectMapper();
//            try {
//                // 解析 JSON 字符串为 Map<String, String>
//                Map<String, String> paramMap = objectMapper.readValue(params, new TypeReference<>() {
//                });
//
//                // 转换为 List<KeyValue> 格式 [{key:"id", value:"1"}, {key:"name", value:"张三"}]
//                List<KeyValue> transformedList = paramMap.entrySet().stream()
//                        .map(entry -> new KeyValue(entry.getKey(), entry.getValue()))
//                        .toList();
//
//                // 将转换后的列表存入 MenuForm
//                formData.setParams(transformedList);
//            } catch (Exception e) {
//                throw new RuntimeException("解析参数失败", e);
//            }
//        }

        return formData;
    }

    /**
     * 新增或更新菜单。
     *
     * <p>方法会根据菜单类型规范化组件与路由字段，校验路由名唯一性，并在保存后刷新角色权限缓存与子节点树路径。</p>
     *
     * @param menuForm 菜单表单
     * @return {@code true} 表示保存成功
     */
    @Override
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public boolean saveMenu(MenuForm menuForm) {

        Integer menuType = menuForm.getType();

        if (MenuTypeEnum.CATALOG.getValue().equals(menuType)) {  // 如果是目录
            String path = menuForm.getRoutePath();
            if (menuForm.getParentId() == 0 && !path.startsWith("/")) {
                menuForm.setRoutePath("/" + path); // 一级目录需以 / 开头
            }
            menuForm.setComponent("Layout");
        } else if (MenuTypeEnum.EXTLINK.getValue().equals(menuType)) {
            // 外链菜单组件设置为 null
            menuForm.setComponent(null);
        }

        if (Objects.equals(menuForm.getParentId(), menuForm.getId())) {
            throw new RuntimeException("父级菜单不能为当前菜单");
        }

        MenuPO menu = menuConverter.toPo(menuForm);
        String treePath = generateMenuTreePath(menuForm.getParentId());
        menu.setTreePath(treePath);

        // 新增类型为菜单时候 路由名称唯一
        if (MenuTypeEnum.MENU.getValue().equals(menuType)) {
            Assert.isFalse(this.exists(new LambdaQueryWrapper<MenuPO>()
                    .eq(MenuPO::getRouteName, menu.getRouteName())
                    .ne(menuForm.getId() != null, MenuPO::getId, menuForm.getId())
            ), "路由名称已存在");
            if (Objects.equals(menuForm.getParentId(), SystemConstants.ROOT_NODE_ID)) {
                throw new RuntimeException("菜单类型不能为顶级菜单");
            }
        } else {
            // 其他类型时 给路由名称赋值为空
            menu.setRouteName(null);
        }
        boolean result = saveOrUpdateMenu(menu, menuForm.getPerms());
        if (result) {
            // 编辑刷新角色权限缓存
            if (menuForm.getId() != null) {
                roleMenuService.refreshRolePermsCache();
            }
        }
        // 修改菜单如果有子菜单，则更新子菜单的树路径
        updateChildrenTreePath(menu.getId(), treePath);
        return result;
    }

    /**
     * 批量删除菜单并刷新角色权限缓存。
     *
     * @param ids 菜单 ID 列表
     * @return {@code true} 表示删除成功
     */
    @Override
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public boolean deleteMenu(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }

        boolean result = this.removeBatchByIds(ids);

        // 刷新角色权限缓存
        if (result) {
            roleMenuService.refreshRolePermsCache();
        }
        return result;
    }


    /**
     * 保存或更新菜单，并同步其按钮权限。
     *
     * @param menu        菜单实体
     * @param permitForms 按钮权限表单列表
     * @return {@code true} 表示保存成功
     */
    private boolean saveOrUpdateMenu(MenuPO menu, List<PermitForm> permitForms) {
        if (MenuTypeEnum.MENU.getValue().equals(menu.getType())) {
            List<MenuPO> permit = menuConverter.toPos(permitForms);
            List<MenuPO> oldPermit = this.list(new LambdaQueryWrapper<MenuPO>()
                    .eq(MenuPO::getParentId, menu.getId())
                    .eq(MenuPO::getType, MenuTypeEnum.BUTTON.getValue())
            );
            List<Long> oldIds = oldPermit.stream().map(MenuPO::getId).toList();
            this.removeBatchByIds(oldIds);
            this.saveBatch(permit);
        }
        menu.setPerm(null);
        return this.saveOrUpdate(menu);
    }

    /**
     * 递归更新子菜单树路径。
     *
     * @param id       当前菜单 ID
     * @param treePath 当前菜单树路径
     */
    private void updateChildrenTreePath(Long id, String treePath) {
        List<MenuPO> children = this.list(new LambdaQueryWrapper<MenuPO>().eq(MenuPO::getParentId, id));
        if (CollectionUtil.isNotEmpty(children)) {
            // 子菜单的树路径等于父菜单的树路径加上父菜单ID
            String childTreePath = treePath + "," + id;
            this.update(new LambdaUpdateWrapper<MenuPO>()
                    .eq(MenuPO::getParentId, id)
                    .set(MenuPO::getTreePath, childTreePath)
            );
            for (MenuPO child : children) {
                // 递归更新子菜单
                updateChildrenTreePath(child.getId(), childTreePath);
            }
        }
    }

    /**
     * 生成菜单树路径。
     *
     * @param parentId 父菜单 ID
     * @return 父路径字符串（逗号分隔），例如 {@code 1,2,3}
     */
    private String generateMenuTreePath(Long parentId) {
        if (SystemConstants.ROOT_NODE_ID.equals(parentId)) {
            return String.valueOf(parentId);
        } else {
            MenuPO parent = this.getById(parentId);
            return parent != null ? parent.getTreePath() + "," + parent.getId() : null;
        }
    }

    /**
     * 递归构建路由树。
     *
     * @param parentId        父节点 ID
     * @param menuChildrenMap 菜单列表
     * @return 路由树列表
     */
    private List<RouteVO> buildRoutes(Long parentId, Map<Long, List<MenuPO>> menuChildrenMap) {
        List<MenuPO> childrenMenus = menuChildrenMap.get(parentId);
        if (CollectionUtil.isEmpty(childrenMenus)) {
            return Collections.emptyList();
        }
        List<RouteVO> routeList = new ArrayList<>(childrenMenus.size());
        for (MenuPO menu : childrenMenus) {
            RouteVO routeVO = toRouteVo(menu);
            List<RouteVO> children = buildRoutes(menu.getId(), menuChildrenMap);
            if (!children.isEmpty()) {
                routeVO.setChildren(children);
            }
            routeList.add(routeVO);
        }
        return routeList;
    }

    /**
     * 将菜单实体转换为前端路由对象。
     *
     * @param menu 菜单实体
     * @return 路由对象
     */
    private RouteVO toRouteVo(MenuPO menu) {
        RouteVO routeVO = new RouteVO();
        // 获取路由名称
        String routeName = menu.getRouteName();
        if (StrUtil.isBlank(routeName)) {
            // 路由 name 需要驼峰，首字母大写
            routeName = StringUtils.capitalize(StrUtil.toCamelCase(menu.getRoutePath(), '-'));
        }
        // 根据name路由跳转 this.$router.push({name:xxx})
        routeVO.setName(routeName);

        // 根据path路由跳转 this.$router.push({path:xxx})
        routeVO.setPath(menu.getRoutePath());
        routeVO.setRedirect(menu.getRedirect());
        routeVO.setComponent(menu.getComponent());

        RouteVO.Meta meta = new RouteVO.Meta();
        meta.setTitle(menu.getName());
        meta.setIcon(menu.getIcon());
        meta.setHidden(StatusEnum.DISABLE.getValue().equals(menu.getVisible()));
        // 【菜单】是否开启页面缓存
        if (MenuTypeEnum.MENU.getValue().equals(menu.getType())
                && ObjectUtil.equals(menu.getKeepAlive(), 1)) {
            meta.setKeepAlive(true);
        }
        meta.setAlwaysShow(ObjectUtil.equals(menu.getAlwaysShow(), 1));

//        String paramsJson = menu.getParams();
//        // 将 JSON 字符串转换为 Map<String, String>
//        if (StrUtil.isNotBlank(paramsJson)) {
//            ObjectMapper objectMapper = new ObjectMapper();
//            try {
//                Map<String, String> paramMap = objectMapper.readValue(paramsJson, new TypeReference<>() {
//                });
//                meta.setParams(paramMap);
//            } catch (Exception e) {
//                throw new RuntimeException("解析参数失败", e);
//            }
//        }
        routeVO.setMeta(meta);
        return routeVO;
    }

    /**
     * 递归构建菜单下拉树。
     *
     * @param parentId        父节点 ID
     * @param menuChildrenMap 菜单列表
     * @return 菜单选项树
     */
    private List<Option<Long>> buildMenuOptions(Long parentId, Map<Long, List<MenuPO>> menuChildrenMap) {
        List<MenuPO> childrenMenus = menuChildrenMap.get(parentId);
        if (CollectionUtil.isEmpty(childrenMenus)) {
            return Collections.emptyList();
        }
        List<Option<Long>> menuOptions = new ArrayList<>(childrenMenus.size());
        for (MenuPO menu : childrenMenus) {
            Option<Long> option = new Option<>(menu.getId(), menu.getName());
            List<Option<Long>> subMenuOptions = buildMenuOptions(menu.getId(), menuChildrenMap);
            if (!subMenuOptions.isEmpty()) {
                option.setChildren(subMenuOptions);
            }
            menuOptions.add(option);
        }

        return menuOptions;
    }

    /**
     * 递归构建菜单展示树。
     *
     * @param parentId        父节点 ID
     * @param menuChildrenMap 菜单列表
     * @return 菜单树列表
     */
    private List<MenuVO> buildMenuTree(Long parentId, Map<Long, List<MenuPO>> menuChildrenMap) {
        List<MenuPO> childrenMenus = menuChildrenMap.get(parentId);
        if (CollectionUtil.isEmpty(childrenMenus)) {
            return Collections.emptyList();
        }
        return childrenMenus.stream()
                .map(entity -> {
                    MenuVO menuVO = menuConverter.toVo(entity);
                    List<MenuVO> children = buildMenuTree(entity.getId(), menuChildrenMap);
                    menuVO.setChildren(children);
                    return menuVO;
                }).toList();
    }

    /**
     * 按父节点分组菜单，避免递归时重复全量扫描。
     *
     * @param menuList 菜单列表
     * @return 父节点到子菜单列表映射
     */
    private Map<Long, List<MenuPO>> groupMenusByParentId(List<MenuPO> menuList) {
        return CollectionUtil.emptyIfNull(menuList)
                .stream()
                .collect(Collectors.groupingBy(MenuPO::getParentId));
    }
}
