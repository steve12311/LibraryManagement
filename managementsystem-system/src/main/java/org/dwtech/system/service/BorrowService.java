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
    IPage<BorrowVO> getBorrowPage(@Valid BorrowPageQuery queryParams);

    boolean saveBorrow(@Valid BorrowForm formData);

    boolean updateBorrow(String borrowId, @Valid BorrowForm formData);
}
