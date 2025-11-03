package org.dwtech.service;

import org.dwtech.common.core.entity.dto.LibPublishDto;

import java.util.List;

public interface LibPublishService {
    List<LibPublishDto> selectPublishByIds(Long[] ids);
}
