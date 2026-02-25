package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.vo.StockPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockConverter {
    @Mappings({
            @Mapping(source = "pressName", target = "publishName"),
            @Mapping(source = "stock", target = "stockNumber"),
            @Mapping(source = "currentStock", target = "currentNumber"),
            @Mapping(source = "cover", target = "bookImage")
    })
    StockPageVO toPageVo(StockBO bo);

    StockBO toBo(StockForm form);

    StockPO toPo(StockBO bo);

    StockPO toPo(StockForm form);

    BookPO toBookPo(StockBO bo);

    Page<StockPageVO> toPageVo(Page<StockBO> bo);

    List<StockPageVO> toListVo(List<StockBO> bo);

    StockForm toForm(StockBO stock);
}
