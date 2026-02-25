package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.CategoryPO;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryVO;

import java.util.List;
/**
 * CategoryService
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface CategoryService extends IService<CategoryPO> {
    List<CategoryVO> listCategories(CategoryQuery queryParams);

    List<Option<Long>> listCategoryOptions();
}
