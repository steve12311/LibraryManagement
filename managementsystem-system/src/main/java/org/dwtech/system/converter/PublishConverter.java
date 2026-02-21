package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.form.PublishForm;
import org.dwtech.common.core.entity.po.PublishPO;
import org.dwtech.common.core.entity.vo.PublishPageVO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PublishConverter {
    @AfterMapping
    default void setAddressFromComponents(@MappingTarget PublishPageVO vo, PublishPO po) {
        StringBuilder addressBuilder = new StringBuilder();

        // 处理省/直辖市
        if (po.getProvince() != null && !po.getProvince().isEmpty()) {
            addressBuilder.append(po.getProvince());
        }

        // 处理市（直辖市特殊处理）
        if (po.getCity() != null && !po.getCity().isEmpty()) {
            // 判断是否为直辖市
            boolean isMunicipality = isMunicipality(po.getProvince());

            // 如果不是直辖市，或者虽然是直辖市但市和省不同，则添加市
            if (!isMunicipality || !po.getCity().equals(po.getProvince())) {
                addressBuilder.append(po.getCity());
            }
        }

        // 处理区
        if (po.getArea() != null && !po.getArea().isEmpty()) {
            addressBuilder.append(po.getArea());
        }

        // 处理详细地址
        if (po.getAreaDetail() != null && !po.getAreaDetail().isEmpty()) {
            addressBuilder.append(po.getAreaDetail());
        }

        vo.setAddress(addressBuilder.toString());
    }

    /**
     * 判断是否为直辖市
     */
    default boolean isMunicipality(String province) {
        if (province == null) return false;

        // 直辖市列表
        String[] municipalities = {"北京市", "天津市", "上海市", "重庆市"};

        for (String municipality : municipalities) {
            if (municipality.equals(province)) {
                return true;
            }
        }
        return false;
    }

    @Mappings({
            @Mapping(source = "id", target = "publishId"),
            @Mapping(source = "name", target = "publishName"),
            @Mapping(source = "postalCode", target = "addressCode"),
            @Mapping(source = "telephone", target = "phonenumber")
    })
    PublishPageVO toPageVo(PublishPO po);

    Page<PublishPageVO> toPageVo(Page<PublishPO> po);

    PublishForm toForm(PublishPO publish);

    PublishPO toPo(PublishForm publish);
}
