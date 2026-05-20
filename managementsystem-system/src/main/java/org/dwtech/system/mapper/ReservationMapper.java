package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.MyReservationBO;
import org.dwtech.system.model.bo.ReservationBO;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.model.query.MyReservationPageQuery;
import org.dwtech.system.model.query.ReservationPageQuery;
import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<ReservationPO> {

    Page<ReservationBO> getReservationPage(Page<ReservationBO> page,
                                           @Param("queryParams") ReservationPageQuery queryParams);

    Page<MyReservationBO> getMyReservationPage(Page<MyReservationBO> page,
                                               @Param("userId") Long userId,
                                               @Param("queryParams") MyReservationPageQuery queryParams);

    List<ReservationBO> getBookReservationQueue(@Param("isbn") String isbn);

    int countActiveByUser(@Param("userId") Long userId);

    int countActiveByIsbn(@Param("isbn") String isbn);

    ReservationPO selectFirstPendingByIsbn(@Param("isbn") String isbn);

    List<ReservationPO> selectExpiredReady();
}
