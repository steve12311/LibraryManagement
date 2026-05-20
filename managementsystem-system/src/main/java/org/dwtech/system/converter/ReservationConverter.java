package org.dwtech.system.converter;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.MyReservationBO;
import org.dwtech.system.model.bo.ReservationBO;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.model.form.ReservationForm;
import org.dwtech.system.model.vo.AdminReservationPageVO;
import org.dwtech.system.model.vo.ReservationPageVO;
import org.dwtech.system.model.vo.ReservationQueueVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationConverter {

    @Mapping(source = "id", target = "id")
    AdminReservationPageVO toAdminPageVo(ReservationBO bo);

    Page<AdminReservationPageVO> toAdminPageVo(Page<ReservationBO> page);

    @Mapping(source = "id", target = "id")
    ReservationPageVO toPageVo(MyReservationBO bo);

    Page<ReservationPageVO> toMyPageVo(Page<MyReservationBO> page);

    @Mapping(source = "id", target = "id")
    ReservationQueueVO toQueueVo(ReservationBO bo);

    ReservationPO toPo(ReservationForm form);
}
