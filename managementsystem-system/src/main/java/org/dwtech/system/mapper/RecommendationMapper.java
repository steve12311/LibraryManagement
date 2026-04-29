package org.dwtech.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.BookBorrowFreqBO;
import org.dwtech.system.model.bo.CoBorrowCountBO;

import java.util.List;

/**
 * 协同过滤推荐数据访问层
 *
 * @author steve12311
 * @since 2026-04-29
 */
@Mapper
public interface RecommendationMapper {
    /**
     * 查询每本图书的去重借阅人数。
     *
     * @return 图书借阅人数统计
     */
    List<BookBorrowFreqBO> selectBookBorrowFrequency();

    /**
     * 查询达到最小支持度的共借图书对。
     *
     * @return 共借图书对统计
     */
    List<CoBorrowCountBO> selectCoBorrowPairs();

    /**
     * 查询用户历史借阅 ISBN。
     *
     * @param userId 用户 ID
     * @return 去重 ISBN 列表
     */
    List<String> selectUserBorrowedIsbns(@Param("userId") Long userId);
}
