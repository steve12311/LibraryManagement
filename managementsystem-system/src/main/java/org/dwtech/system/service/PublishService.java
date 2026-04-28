package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PublishForm;
import org.dwtech.system.model.query.PublishPageQuery;
import org.dwtech.system.model.vo.PublishPageVO;

import java.util.List;
/**
 * 出版社管理服务，提供出版社的分页查询、表单查询、新增、更新、删除及下拉选项功能。
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface PublishService {
    /**
     * 分页查询出版社列表。
     *
     * @param queryParams 分页查询参数（页码、每页条数、筛选条件）
     * @return 出版社分页结果
     */
    IPage<PublishPageVO> getPublishPage(PublishPageQuery queryParams);

    /**
     * 根据 ID 查询出版社表单数据（用于编辑回显）。
     *
     * @param id 出版社主键 ID
     * @return 出版社表单
     */
    PublishForm getPublishForm(Long id);

    /**
     * 新增出版社。新增后清除出版社列表缓存。
     *
     * @param publishForm 出版社表单
     * @return true 表示新增成功，false 表示失败
     */
    boolean savePublish(@Valid PublishForm publishForm);

    /**
     * 批量删除出版社。删除后清除出版社列表缓存。
     *
     * @param ids 待删除的出版社主键 ID 列表
     * @return true 表示全部删除成功，false 表示失败
     */
    boolean deletePublish(List<Long> ids);

    /**
     * 查询所有出版社的下拉选项列表，供前端下拉选择器使用。
     *
     * @return 出版社选项列表
     */
    List<Option<Long>> listPublishOptions();

    /**
     * 更新出版社信息。更新后清除出版社列表缓存。
     *
     * @param publishForm 出版社表单（需包含 ID）
     * @return true 表示更新成功，false 表示失败
     */
    boolean updatePublish(@Valid PublishForm publishForm);
}
