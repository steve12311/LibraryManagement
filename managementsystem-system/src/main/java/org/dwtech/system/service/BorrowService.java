package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.common.core.entity.form.BorrowForm;
import org.dwtech.common.core.entity.query.BorrowPageQuery;
import org.dwtech.common.core.entity.vo.BorrowVO;

public interface BorrowService {
    IPage<BorrowVO> getBorrowPage(@Valid BorrowPageQuery queryParams);

    boolean saveBorrow(@Valid BorrowForm formData);

    boolean updateBorrow(String borrowId, @Valid BorrowForm formData);
}
