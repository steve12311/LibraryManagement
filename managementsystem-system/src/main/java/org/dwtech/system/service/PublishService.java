package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.common.core.entity.form.PublishForm;
import org.dwtech.common.core.entity.query.PublishPageQuery;
import org.dwtech.common.core.entity.vo.PublishPageVO;

import java.util.List;

public interface PublishService {
    IPage<PublishPageVO> getPublishPage(PublishPageQuery queryParams);

    PublishForm getPublishForm(Long id);

    boolean savePublish(@Valid PublishForm publishForm);

    boolean deletePublish(List<Long> ids);
}
