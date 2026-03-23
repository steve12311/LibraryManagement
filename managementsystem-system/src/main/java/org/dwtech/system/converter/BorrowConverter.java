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
    /**
     * 用途：转换为 vo。
     * 
     * @param borrowBO borrow bo
     * @return 返回结果
     */
    BorrowVO toVo(BorrowBO borrowBO);

    @Mappings({
            @Mapping(source = "id", target = "borrowId"),
            @Mapping(target = "cover", expression = "java(normalizeCoverUrl(myBorrowBO.getCover()))")
    })
    /**
     * 用途：转换为当前登录用户借阅订单 vo。
     *
     * @param myBorrowBO my borrow bo
     * @return 返回结果
     */
    MyBorrowPageVO toMyBorrowPageVo(MyBorrowBO myBorrowBO);

    /**
     * 用途：转换为 po。
     * 
     * @param borrowForm borrow form
     * @return 返回结果
     */
    BorrowPO toPo(BorrowForm borrowForm);

    /**
     * 用途：转换为 page vo。
     * 
     * @param borrowPage borrow page
     * @return 分页结果
     */
    Page<BorrowVO> toPageVo(Page<BorrowBO> borrowPage);

    /**
     * 用途：转换为当前登录用户借阅订单分页视图。
     *
     * @param borrowPage borrow page
     * @return 分页结果
     */
    Page<MyBorrowPageVO> toMyBorrowPageVo(Page<MyBorrowBO> borrowPage);

    /**
     * 用途：规范借阅订单中的封面路径。
     *
     * @param cover 原始封面路径
     * @return 可直接访问的封面路径
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
