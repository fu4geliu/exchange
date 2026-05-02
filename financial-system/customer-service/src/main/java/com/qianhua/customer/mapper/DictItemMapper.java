package com.qianhua.customer.mapper;

import com.qianhua.customer.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictItemMapper {

    List<DictItem> listByDictCode(@Param("dictCode") String dictCode);

    int countItem(@Param("dictCode") String dictCode, @Param("itemCode") String itemCode);
}
