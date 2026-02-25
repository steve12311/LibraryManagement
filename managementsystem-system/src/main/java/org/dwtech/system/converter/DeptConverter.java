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
    /**
     * 用途：转换为 vo。
     * 
     * @param dept dept
     * @return 返回结果
     */
    DeptVO toVo(DeptPO dept);
}
