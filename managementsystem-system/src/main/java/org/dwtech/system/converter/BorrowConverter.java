package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.BorrowBO;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.vo.BorrowVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
/**
 * BorrowConverter
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Mapper(componentModel = "spring")
public interface BorrowConverter {
    @Mappings({
            @Mapping(source = "id", target = "borrowId")
    })
    BorrowVO toVo(BorrowBO borrowBO);

    BorrowPO toPo(BorrowForm borrowForm);

    Page<BorrowVO> toPageVo(Page<BorrowBO> borrowPage);
}
