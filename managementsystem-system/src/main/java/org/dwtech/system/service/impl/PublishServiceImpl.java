package org.dwtech.system.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.PublishForm;
import org.dwtech.common.core.entity.po.PublishPO;
import org.dwtech.common.core.entity.query.PublishPageQuery;
import org.dwtech.common.core.entity.vo.PublishPageVO;
import org.dwtech.system.converter.PublishConverter;
import org.dwtech.system.mapper.PublishMapper;
import org.dwtech.system.service.PublishService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends ServiceImpl<PublishMapper, PublishPO> implements PublishService {
    private final PublishConverter publishConverter;

    @Override
    public IPage<PublishPageVO> getPublishPage(PublishPageQuery queryParams) {
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<PublishPO> page = new Page<>(pageNum, pageSize);

        Page<PublishPO> publish = this.baseMapper.getPublishPage(page, queryParams);

        return publishConverter.toPageVo(publish);
    }

    @Override
    public PublishForm getPublishForm(Long id) {
        PublishPO publish = this.getById(id);
        Assert.isTrue(publish != null, "出版社不存在");
        return publishConverter.toForm(publish);
    }

    @Override
    public boolean savePublish(PublishForm publishForm) {
        PublishPO publish = publishConverter.toPo(publishForm);
        return this.save(publish);
    }

    @Override
    public boolean deletePublish(List<Long> ids) {
        Assert.isTrue(ArrayUtil.isNotEmpty(ids), "删除的出版社数据为空");
        return this.removeByIds(ids);
    }

    @Override
    public List<Option<Long>> getPublishOptions() {
        List<PublishPO> list = this.list();
        return publishConverter.toOptions(list);
    }

    @Override
    public boolean updatePublish(PublishForm publishForm) {
        PublishPO publish = publishConverter.toPo(publishForm);
        return this.updateById(publish);
    }
}
