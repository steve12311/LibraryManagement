package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.bo.BorrowBO;
import org.dwtech.common.core.entity.form.BorrowForm;
import org.dwtech.common.core.entity.po.BorrowPO;
import org.dwtech.common.core.entity.vo.BorrowVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface BorrowConverter {
    @Mappings({
            @Mapping(source = "id", target = "borrowId")
    })
    BorrowVO toVo(BorrowBO borrowBO);

    BorrowPO toPo(BorrowForm borrowForm);

    Page<BorrowVO> toPageVo(Page<BorrowBO> borrowPage);
}
