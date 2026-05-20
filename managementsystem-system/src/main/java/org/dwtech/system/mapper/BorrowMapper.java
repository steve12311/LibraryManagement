package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.BorrowBO;
import java.util.List;
import org.dwtech.system.model.bo.MyBorrowBO;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.query.BorrowPageQuery;
import org.dwtech.system.model.query.MyBorrowPageQuery;
/**
 * 借阅订单数据访问层，提供借阅记录的分页查询接口
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Mapper
public interface BorrowMapper extends BaseMapper<BorrowPO> {
    /**
     * 分页查询借阅记录列表，支持多种筛选条件
     *
     * @return 分页结果
     */
    Page<BorrowBO> getBorrowPage(Page<BorrowBO> page, @Param("queryParams") BorrowPageQuery queryParams);

    /**
     * 分页查询当前登录用户的借阅订单
     *
     * @param userId 当前登录用户 ID
     * @return 分页结果
     */
    Page<MyBorrowBO> getCurrentUserBorrowPage(Page<MyBorrowBO> page,
                                              @Param("userId") Long userId,
                                              @Param("queryParams") MyBorrowPageQuery queryParams);

    /**
     * 查询指定天数后到期的未还借阅（用于到期提醒）
     *
     * @param days 距到期天数（3 或 1）
     * @return 待提醒的借阅记录
     */
    List<BorrowPO> selectDueSoon(@Param("days") int days);

    /**
     * 查询已逾期的未还借阅（用于逾期通知）。
     * SQL: reality_return_time IS NULL AND return_time &lt; CURDATE()
     *
     * @return 已逾期的借阅记录
     */
    List<BorrowPO> selectOverdue();
}
