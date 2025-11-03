package org.dwtech.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.po.LibPublishPo;

import java.util.List;

@Mapper
public interface LibPublishMapper extends BaseMapper<LibPublishPo> {
    List<LibPublishPo> selectPublishByIds(Long[] ids);

    IPage<LibPublishPo> selectPublishList(IPage<LibPublishPo> page, @Param("libPublishPo") LibPublishPo libPublishPo);

    Integer insertPublish(LibPublishPo publishPo);

    Integer updatePublish(LibPublishPo publishPo);

    Integer deletePublish(Long id);
}
