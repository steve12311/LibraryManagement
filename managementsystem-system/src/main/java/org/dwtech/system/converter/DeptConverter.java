package org.dwtech.system.converter;

import org.dwtech.common.core.entity.po.DeptPO;
import org.dwtech.common.core.entity.vo.DeptVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeptConverter {
    DeptVO toVo(DeptPO dept);
}
