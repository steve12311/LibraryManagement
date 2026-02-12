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
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.MenuForm;
import org.dwtech.common.core.entity.form.PermitForm;
import org.dwtech.common.core.entity.po.MenuPO;
import org.dwtech.common.core.entity.query.MenuQuery;
import org.dwtech.common.core.entity.vo.MenuVO;
import org.dwtech.common.core.entity.vo.RouteVO;
import org.dwtech.common.enmus.MenuTypeEnum;
import org.dwtech.common.enmus.StatusEnum;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.converter.MenuConverter;
import org.dwtech.system.mapper.MenuMapper;
import org.dwtech.system.service.MenuService;
import org.dwtech.system.service.RoleMenuService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuPO> implements MenuService {
    private final MenuConverter menuConverter;
    private final RoleMenuService roleMenuService;

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

        // 使用递归函数来构建菜单树
        return rootIds.stream()
                .flatMap(rootId -> buildMenuTree(rootId, menus).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<Option<Long>> listMenuOptions(boolean onlyParent) {
        List<MenuPO> menuList = this.list(new LambdaQueryWrapper<MenuPO>()
                .in(onlyParent, MenuPO::getType, MenuTypeEnum.CATALOG.getValue())
                .orderByAsc(MenuPO::getSort)
        );
        return buildMenuOptions(SystemConstants.ROOT_NODE_ID, menuList);
    }

    @Override
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
        return buildRoutes(SystemConstants.ROOT_NODE_ID, menuList);
    }

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

    @Override
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

    @Override
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
     * 更新子菜单树路径
     *
     * @param id       当前菜单ID
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
     * 路径生成
     *
     * @param parentId 父ID
     * @return 父节点路径以英文逗号(, )分割，eg: 1,2,3
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
     * 递归生成菜单路由层级列表
     *
     * @param parentId 父级ID
     * @param menuList 菜单列表
     * @return 路由层级列表
     */
    private List<RouteVO> buildRoutes(Long parentId, List<MenuPO> menuList) {
        List<RouteVO> routeList = new ArrayList<>();

        for (MenuPO menu : menuList) {
            if (menu.getParentId().equals(parentId)) {
                RouteVO routeVO = toRouteVo(menu);
                List<RouteVO> children = buildRoutes(menu.getId(), menuList);
                if (!children.isEmpty()) {
                    routeVO.setChildren(children);
                }
                routeList.add(routeVO);
            }
        }

        return routeList;
    }

    /**
     * 根据RouteBO创建RouteVO
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
     * 递归生成菜单下拉层级列表
     *
     * @param parentId 父级ID
     * @param menuList 菜单列表
     * @return 菜单下拉列表
     */
    private List<Option<Long>> buildMenuOptions(Long parentId, List<MenuPO> menuList) {
        List<Option<Long>> menuOptions = new ArrayList<>();

        for (MenuPO menu : menuList) {
            if (menu.getParentId().equals(parentId)) {
                Option<Long> option = new Option<>(menu.getId(), menu.getName());
                List<Option<Long>> subMenuOptions = buildMenuOptions(menu.getId(), menuList);
                if (!subMenuOptions.isEmpty()) {
                    option.setChildren(subMenuOptions);
                }
                menuOptions.add(option);
            }
        }

        return menuOptions;
    }

    /**
     * 递归生成菜单列表
     *
     * @param parentId 父级ID
     * @param menuList 菜单列表
     * @return 菜单列表
     */
    private List<MenuVO> buildMenuTree(Long parentId, List<MenuPO> menuList) {
        return CollectionUtil.emptyIfNull(menuList)
                .stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .map(entity -> {
                    MenuVO menuVO = menuConverter.toVo(entity);
                    List<MenuVO> children = buildMenuTree(entity.getId(), menuList);
                    menuVO.setChildren(children);
                    return menuVO;
                }).toList();
    }
}
