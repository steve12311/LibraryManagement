package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.BookshelfUsageBO;
import org.dwtech.system.model.entity.BookshelfPO;
import org.dwtech.system.model.vo.BookshelfOptionVO;
import org.dwtech.system.model.vo.PublicShelfBookVO;

import java.util.List;

/**
 * 图书馆书架数据访问层
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Mapper
public interface BookshelfMapper extends BaseMapper<BookshelfPO> {

    int countByFloorId(@Param("floorId") Long floorId);

    int countStockByShelfId(@Param("shelfId") Long shelfId);

    Integer sumStockByShelfId(@Param("shelfId") Long shelfId);

    List<BookshelfUsageBO> sumStockByShelfIds(@Param("shelfIds") List<Long> shelfIds);

    List<PublicShelfBookVO> listPublicBooksByShelfIds(@Param("shelfIds") List<Long> shelfIds);

    List<BookshelfOptionVO> listShelfOptions(@Param("enabledOnly") boolean enabledOnly);
}
