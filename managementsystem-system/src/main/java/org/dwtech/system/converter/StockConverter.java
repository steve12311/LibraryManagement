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
/**
 * StockConverter
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface StockConverter {
    @Mappings({
            @Mapping(source = "pressName", target = "publishName"),
            @Mapping(source = "stock", target = "stockNumber"),
            @Mapping(source = "currentStock", target = "currentNumber"),
            @Mapping(source = "cover", target = "bookImage")
    })
    /**
     * 用途：转换为 page vo。
     * 
     * @param bo bo
     * @return 返回结果
     */
    StockPageVO toPageVo(StockBO bo);

    /**
     * 用途：转换为 bo。
     * 
     * @param form form
     * @return 返回结果
     */
    StockBO toBo(StockForm form);

    /**
     * 用途：转换为 po。
     * 
     * @param bo bo
     * @return 返回结果
     */
    StockPO toPo(StockBO bo);

    /**
     * 用途：转换为 po。
     * 
     * @param form form
     * @return 返回结果
     */
    StockPO toPo(StockForm form);

    /**
     * 用途：转换为 book po。
     * 
     * @param bo bo
     * @return 返回结果
     */
    BookPO toBookPo(StockBO bo);

    /**
     * 用途：转换为 page vo。
     * 
     * @param bo bo
     * @return 分页结果
     */
    Page<StockPageVO> toPageVo(Page<StockBO> bo);

    /**
     * 用途：转换为 list vo。
     * 
     * @param bo bo
     * @return 结果列表
     */
    List<StockPageVO> toListVo(List<StockBO> bo);

    /**
     * 用途：转换为 form。
     * 
     * @param stock stock
     * @return 返回结果
     */
    StockForm toForm(StockBO stock);
}
