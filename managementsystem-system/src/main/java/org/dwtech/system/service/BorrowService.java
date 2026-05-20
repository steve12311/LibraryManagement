package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.query.BorrowPageQuery;
import org.dwtech.system.model.query.MyBorrowPageQuery;
import org.dwtech.system.model.vo.BorrowVO;
import org.dwtech.system.model.vo.MyBorrowPageVO;
/**
 * 借阅管理服务，负责借阅记录的列表查询、新增借阅、归还及续借操作。
 *
 * @author steve12311
 * @since 2026-02-24
 */

public interface BorrowService {
    /**
     * 分页查询全部借阅记录，支持按用户、图书、借阅状态等条件筛选。
     *
     * @param queryParams 分页查询参数（页码、每页条数、筛选条件）
     * @return 借阅记录分页结果
     */
    IPage<BorrowVO> getBorrowPage(@Valid BorrowPageQuery queryParams);

    /**
     * 分页查询当前登录用户的个人借阅记录。
     *
     * @param userId 当前登录用户 ID
     * @param queryParams 分页查询参数（页码、每页条数、筛选条件）
     * @return 个人借阅记录分页结果
     */
    IPage<MyBorrowPageVO> getCurrentUserBorrowPage(Long userId, @Valid MyBorrowPageQuery queryParams);

    /**
     * 新增借阅记录。流程：扣减库存 → 创建借阅记录 → 记录操作日志。
     *
     * @param formData 借阅表单，包含图书、借阅人、借阅日期等信息
     * @return true 表示借阅成功，false 表示失败
     */
    boolean saveBorrow(@Valid BorrowForm formData);

    /**
     * 更新借阅记录（如续借、变更状态等）。
     *
     * @param borrowId 借阅记录 ID
     * @param formData 借阅表单，包含更新后的信息
     * @return true 表示更新成功，false 表示失败
     */
    boolean updateBorrow(String borrowId, @Valid BorrowForm formData);

    /**
     * 管理员手动发送逾期提醒。
     *
     * @param borrowId 借阅记录 ID
     */
    /** 管理员手动发送逾期提醒（无视去重，自动判断OVERDUE/OVERDUE_REMINDER） */
    void sendReminder(String borrowId);
}
