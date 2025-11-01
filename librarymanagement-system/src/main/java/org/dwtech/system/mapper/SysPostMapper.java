package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.po.SysPostPo;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPostPo> {

    IPage<SysPostPo> selectPostList(IPage<SysPostPo> page, @Param("sysPost") SysPostPo sysPost);

    SysPostPo selectPostById(Long id);

    Integer hasPost(SysPostPo postPo);

    Integer insertPost(SysPostPo postPo);

    Integer updatePost(SysPostPo sysPostPo);

    Integer deletePostById(Long postId);
}
