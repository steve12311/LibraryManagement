package org.dwtech.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.po.LibCategoryPo;

@Mapper
public interface LibCategoryMapper extends BaseMapper<LibCategoryPo> {
    IPage<LibCategoryPo> selectLibCategoryList(IPage<LibCategoryPo> page, @Param("libCategory") LibCategoryPo libCategory);
}
