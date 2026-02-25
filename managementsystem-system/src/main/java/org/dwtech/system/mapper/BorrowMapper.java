package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.BorrowBO;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.query.BorrowPageQuery;

@Mapper
public interface BorrowMapper extends BaseMapper<BorrowPO> {
    Page<BorrowBO> getBorrowPage(Page<BorrowBO> page, @Param("queryParams") BorrowPageQuery queryParams);
}
