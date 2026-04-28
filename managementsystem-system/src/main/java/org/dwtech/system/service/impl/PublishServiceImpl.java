package org.dwtech.system.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PublishForm;
import org.dwtech.system.model.entity.PublishPO;
import org.dwtech.system.model.query.PublishPageQuery;
import org.dwtech.system.model.vo.PublishPageVO;
import org.dwtech.system.converter.PublishConverter;
import org.dwtech.system.mapper.PublishMapper;
import org.dwtech.system.service.PublishService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * PublishServiceImpl
 * 出版社管理服务实现。通过 PublishConverter 完成 PO/Form/VO 互转，
 * 查询操作结合 Spring Cache 进行缓存，写操作清除缓存。
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends ServiceImpl<PublishMapper, PublishPO> implements PublishService {
    private final PublishConverter publishConverter;

    /**
     * 分页查询出版社列表。使用 MyBatis-Plus 分页插件，通过 Converter 转为 VO 分页结果。
     *
     * @param queryParams 分页查询参数
     * @return 出版社分页结果
     */
    @Override
    public IPage<PublishPageVO> getPublishPage(PublishPageQuery queryParams) {
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<PublishPO> page = new Page<>(pageNum, pageSize);

        Page<PublishPO> publish = this.baseMapper.getPublishPage(page, queryParams);

        return publishConverter.toPageVo(publish);
    }

    /**
     * 根据 ID 查询出版社表单。校验出版社是否存在，通过 Converter 转为 Form 返回。
     *
     * @param id 出版社主键 ID
     * @return 出版社表单
     */
    @Override
    public PublishForm getPublishForm(Long id) {
        PublishPO publish = this.getById(id);
        Assert.isTrue(publish != null, "出版社不存在");
        return publishConverter.toForm(publish);
    }

    /**
     * 新增出版社。通过 Converter 转为 PO 后保存，保存后清除 publish 缓存。
     *
     * @param publishForm 出版社表单
     * @return true 表示新增成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = "publish", allEntries = true)
    public boolean savePublish(PublishForm publishForm) {
        PublishPO publish = publishConverter.toPo(publishForm);
        return this.save(publish);
    }

    /**
     * 批量删除出版社。校验列表不为空后删除，删除后清除 publish 缓存。
     *
     * @param ids 待删除的出版社主键 ID 列表
     * @return true 表示全部删除成功
     */
    @Override
    @CacheEvict(cacheNames = "publish", allEntries = true)
    public boolean deletePublish(List<Long> ids) {
        Assert.isTrue(ArrayUtil.isNotEmpty(ids), "删除的出版社数据为空");
        return this.removeByIds(ids);
    }

    /**
     * 查询全部出版社的下拉选项列表。通过 Converter 将 PO 列表转为 Option 列表，结果被 Spring Cache 缓存。
     *
     * @return 出版社选项列表
     */
    @Override
    @Cacheable(cacheNames = "publish", key = "'options'")
    public List<Option<Long>> listPublishOptions() {
        List<PublishPO> list = this.list();
        return publishConverter.toOptions(list);
    }

    /**
     * 更新出版社信息。通过 Converter 转为 PO 后按 ID 更新，更新后清除 publish 缓存。
     *
     * @param publishForm 出版社表单（需包含 ID）
     * @return true 表示更新成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = "publish", allEntries = true)
    public boolean updatePublish(PublishForm publishForm) {
        PublishPO publish = publishConverter.toPo(publishForm);
        return this.updateById(publish);
    }
}
