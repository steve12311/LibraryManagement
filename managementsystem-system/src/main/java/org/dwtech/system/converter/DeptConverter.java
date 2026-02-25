package org.dwtech.system.converter;

import org.dwtech.system.model.entity.DeptPO;
import org.dwtech.system.model.vo.DeptVO;
import org.mapstruct.Mapper;
/**
 * DeptConverter
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface DeptConverter {
    DeptVO toVo(DeptPO dept);
}
