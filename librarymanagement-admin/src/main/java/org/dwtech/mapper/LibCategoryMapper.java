package org.dwtech.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.po.LibCategoryPo;

import java.util.List;

@Mapper
public interface LibCategoryMapper extends BaseMapper<LibCategoryPo> {
    List<LibCategoryPo> selectLibCategoryList(LibCategoryPo libCategory);

    List<LibCategoryPo> selectLibCategoryByIds(Long[] categoryIds);

    Integer insertLibCategory(LibCategoryPo libCategoryPo);

    Integer updateLibCategory(LibCategoryPo libCategoryPo);

    Integer deleteLibCategoryById(Long id);
}
