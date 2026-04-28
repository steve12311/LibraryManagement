package org.dwtech.system.converter;

import org.dwtech.system.model.entity.DeptPO;
import org.dwtech.system.model.vo.DeptVO;
import org.mapstruct.Mapper;
/**
 * 部门对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface DeptConverter {
    /**
     * DeptPO → DeptVO
     */
    DeptVO toVo(DeptPO dept);
}
