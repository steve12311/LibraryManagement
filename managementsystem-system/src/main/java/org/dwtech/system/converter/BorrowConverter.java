package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.BorrowBO;
import org.dwtech.system.model.bo.MyBorrowBO;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.vo.BorrowVO;
import org.dwtech.system.model.vo.MyBorrowPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
/**
 * 借阅对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Mapper(componentModel = "spring")
public interface BorrowConverter {
    @Mappings({
            @Mapping(source = "id", target = "borrowId")
    })
    /**
     * BorrowBO → BorrowVO
     */
    BorrowVO toVo(BorrowBO borrowBO);

    @Mappings({
            @Mapping(source = "id", target = "borrowId"),
            @Mapping(target = "cover", expression = "java(normalizeCoverUrl(myBorrowBO.getCover()))")
    })
    /**
     * MyBorrowBO → MyBorrowPageVO
     */
    MyBorrowPageVO toMyBorrowPageVo(MyBorrowBO myBorrowBO);

    /**
     * BorrowForm → BorrowPO
     */
    BorrowPO toPo(BorrowForm borrowForm);

    /**
     * Page<BorrowBO> → Page<BorrowVO>
     */
    Page<BorrowVO> toPageVo(Page<BorrowBO> borrowPage);

    /**
     * Page<MyBorrowBO> → Page<MyBorrowPageVO>
     */
    Page<MyBorrowPageVO> toMyBorrowPageVo(Page<MyBorrowBO> borrowPage);

    /**
     * 规范化借阅订单中的封面路径，确保返回可访问的完整路径
     */
    default String normalizeCoverUrl(String cover) {
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
