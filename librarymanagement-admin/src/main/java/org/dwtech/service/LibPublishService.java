package org.dwtech.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.LibPublishDto;

import java.util.List;

public interface LibPublishService {
    List<LibPublishDto> selectPublishByIds(Long[] ids);

    IPage<LibPublishDto> selectPublishList(LibPublishDto libPublishDto);

    Integer insertPublish(LibPublishDto libPublishDto);

    Integer updatePublish(LibPublishDto libPublishDto);

    Integer deletePublish(Long[] ids);
}
