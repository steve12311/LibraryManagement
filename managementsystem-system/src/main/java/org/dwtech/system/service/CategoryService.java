package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.CategoryPO;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<CategoryPO> {
    List<CategoryVO> listCategories(CategoryQuery queryParams);

    List<Option<Long>> listCategoryOptions();
}
