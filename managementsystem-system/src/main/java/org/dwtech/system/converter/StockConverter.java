package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.model.vo.StockPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * 库存对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface StockConverter {
    @Mappings({
            @Mapping(target = "coverUrl",
                    expression = "java(normalizePublicCoverUrl(bo.getCover()))"),
            @Mapping(source = "pressName", target = "publishName"),
            @Mapping(target = "available",
                    expression = "java(bo.getCurrentStock() != null && bo.getCurrentStock() > 0)")
    })
    /**
     * StockBO → PublicBookPageVO
     */
    PublicBookPageVO toPublicPageVo(StockBO bo);

    @Mappings({
            @Mapping(source = "pressName", target = "publishName"),
            @Mapping(source = "stock", target = "stockNumber"),
            @Mapping(source = "currentStock", target = "currentNumber"),
            @Mapping(source = "cover", target = "bookImage")
    })
    /**
     * StockBO → StockPageVO
     */
    StockPageVO toPageVo(StockBO bo);

    /**
     * StockForm → StockBO
     */
    StockBO toBo(StockForm form);

    /**
     * StockBO → StockPO
     */
    StockPO toPo(StockBO bo);

    /**
     * StockForm → StockPO
     */
    StockPO toPo(StockForm form);

    /**
     * StockBO → BookPO
     */
    BookPO toBookPo(StockBO bo);

    /**
     * Page<StockBO> → Page<StockPageVO>
     */
    Page<StockPageVO> toPageVo(Page<StockBO> bo);

    /**
     * Page<StockBO> → Page<PublicBookPageVO>
     */
    Page<PublicBookPageVO> toPublicPageVo(Page<StockBO> bo);

    /**
     * List<StockBO> → List<StockPageVO>
     */
    List<StockPageVO> toListVo(List<StockBO> bo);

    /**
     * StockBO → StockForm
     */
    StockForm toForm(StockBO stock);

    /**
     * 规范化公开封面访问路径
     */
    default String normalizePublicCoverUrl(String cover) {
        if (cover == null || cover.isBlank()) {
            return cover;
        }
        if (cover.startsWith("/api/v1/files/")) {
            return cover;
        }
        if (cover.startsWith("/")) {
            return "/api/v1/files" + cover;
        }
        return cover;
    }
}
