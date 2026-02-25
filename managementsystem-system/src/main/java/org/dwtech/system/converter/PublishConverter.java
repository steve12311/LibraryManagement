package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PublishForm;
import org.dwtech.system.model.entity.PublishPO;
import org.dwtech.system.model.vo.PublishPageVO;
import org.mapstruct.*;

import java.util.List;
/**
 * PublishConverter
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface PublishConverter {
    /**
     * 用途：执行 set address from components 操作。
     * 
     * @param vo vo
     * @param po po
     * 返回：无。
     */
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
     * 用途：判断 municipality 状态。
     * 
     * 判断是否为直辖市
     * 
     * @param province province
     * @return 操作结果，true 表示成功，false 表示失败
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
    /**
     * 用途：转换为 page vo。
     * 
     * @param po po
     * @return 返回结果
     */
    PublishPageVO toPageVo(PublishPO po);

    /**
     * 用途：转换为 page vo。
     * 
     * @param po po
     * @return 分页结果
     */
    Page<PublishPageVO> toPageVo(Page<PublishPO> po);

    /**
     * 用途：转换为 form。
     * 
     * @param publish publish
     * @return 返回结果
     */
    PublishForm toForm(PublishPO publish);

    /**
     * 用途：转换为 po。
     * 
     * @param publish publish
     * @return 返回结果
     */
    PublishPO toPo(PublishForm publish);

    @Mappings({
            @Mapping(source = "name", target = "label"),
            @Mapping(source = "id", target = "value")
    })
    /**
     * 用途：转换为 option。
     * 
     * @param publish publish
     * @return 返回结果
     */
    Option<Long> toOption(PublishPO publish);

    /**
     * 用途：转换为 options。
     * 
     * @param list list
     * @return 结果列表
     */
    List<Option<Long>> toOptions(List<PublishPO> list);
}
