package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.SysPostDto;

public interface SysPostService {
    IPage<SysPostDto> selectPostList(SysPostDto sysPostDto);

    boolean checkPostNameUnique(SysPostDto sysPostDto);

    Integer insertPost(SysPostDto postDto);

    Integer updatePost(SysPostDto sysPostDto);

    Integer deletePost(Long[] postIds);
}
