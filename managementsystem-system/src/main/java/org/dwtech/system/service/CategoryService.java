package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.core.entity.po.CategoryPO;
import org.dwtech.common.core.entity.query.CategoryQuery;
import org.dwtech.common.core.entity.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<CategoryPO> {
    List<CategoryVO> listMenus(CategoryQuery queryParams);
}
