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
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends ServiceImpl<PublishMapper, PublishPO> implements PublishService {
    private final PublishConverter publishConverter;

    /**
     * 用途：获取 publish page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
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
     * 用途：获取 publish form 信息。
     * 
     * @param id 主键 ID
     * @return 返回结果
     */
    @Override
    public PublishForm getPublishForm(Long id) {
        PublishPO publish = this.getById(id);
        Assert.isTrue(publish != null, "出版社不存在");
        return publishConverter.toForm(publish);
    }

    /**
     * 用途：保存 publish。
     * 
     * @param publishForm publish form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = "publish", allEntries = true)
    public boolean savePublish(PublishForm publishForm) {
        PublishPO publish = publishConverter.toPo(publishForm);
        return this.save(publish);
    }

    /**
     * 用途：删除 publish。
     * 
     * @param ids 主键 ID 列表
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = "publish", allEntries = true)
    public boolean deletePublish(List<Long> ids) {
        Assert.isTrue(ArrayUtil.isNotEmpty(ids), "删除的出版社数据为空");
        return this.removeByIds(ids);
    }

    /**
     * 用途：查询 publish options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    @Override
    @Cacheable(cacheNames = "publish", key = "'options'")
    public List<Option<Long>> listPublishOptions() {
        List<PublishPO> list = this.list();
        return publishConverter.toOptions(list);
    }

    /**
     * 用途：更新 publish。
     * 
     * @param publishForm publish form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = "publish", allEntries = true)
    public boolean updatePublish(PublishForm publishForm) {
        PublishPO publish = publishConverter.toPo(publishForm);
        return this.updateById(publish);
    }
}
