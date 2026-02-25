package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PublishForm;
import org.dwtech.system.model.query.PublishPageQuery;
import org.dwtech.system.model.vo.PublishPageVO;

import java.util.List;
/**
 * PublishService
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface PublishService {
    /**
     * 用途：获取 publish page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    IPage<PublishPageVO> getPublishPage(PublishPageQuery queryParams);

    /**
     * 用途：获取 publish form 信息。
     * 
     * @param id 主键 ID
     * @return 返回结果
     */
    PublishForm getPublishForm(Long id);

    /**
     * 用途：保存 publish。
     * 
     * @param publishForm publish form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean savePublish(@Valid PublishForm publishForm);

    /**
     * 用途：删除 publish。
     * 
     * @param ids 主键 ID 列表
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean deletePublish(List<Long> ids);

    /**
     * 用途：查询 publish options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<Option<Long>> listPublishOptions();

    /**
     * 用途：更新 publish。
     * 
     * @param publishForm publish form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean updatePublish(@Valid PublishForm publishForm);
}
