package org.dwtech.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.dao.LibBookStockDao;

import java.util.List;

@Mapper
public interface LibStockMapper extends BaseMapper<LibBookStockDao> {

    IPage<LibBookStockDao> selectStockList(IPage<LibBookStockDao> page, @Param("libBookStockDao") LibBookStockDao libBookStockDao);

    List<LibBookStockDao> selectStockListByBookIds(Long[] ids);
}
