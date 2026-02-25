package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.query.BorrowPageQuery;
import org.dwtech.system.model.vo.BorrowVO;
/**
 * BorrowService
 *
 * @author steve12311
 * @since 2026-02-24
 */

public interface BorrowService {
    /**
     * 用途：获取 borrow page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    IPage<BorrowVO> getBorrowPage(@Valid BorrowPageQuery queryParams);

    /**
     * 用途：保存 borrow。
     * 
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean saveBorrow(@Valid BorrowForm formData);

    /**
     * 用途：更新 borrow。
     * 
     * @param borrowId borrow ID
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean updateBorrow(String borrowId, @Valid BorrowForm formData);
}
