package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PublishForm;
import org.dwtech.system.model.query.PublishPageQuery;
import org.dwtech.system.model.vo.PublishPageVO;

import java.util.List;

public interface PublishService {
    IPage<PublishPageVO> getPublishPage(PublishPageQuery queryParams);

    PublishForm getPublishForm(Long id);

    boolean savePublish(@Valid PublishForm publishForm);

    boolean deletePublish(List<Long> ids);

    List<Option<Long>> listPublishOptions();

    boolean updatePublish(@Valid PublishForm publishForm);
}
