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
    /**
     * 用途：查询 categories 列表。
     * 
     * @param queryParams query params
     * @return 结果列表
     */
    List<CategoryVO> listCategories(CategoryQuery queryParams);

    /**
     * 用途：查询 category options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<Option<Long>> listCategoryOptions();
}
