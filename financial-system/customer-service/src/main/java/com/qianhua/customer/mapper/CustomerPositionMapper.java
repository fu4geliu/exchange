package com.qianhua.customer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerPositionMapper {

    int insert(
            @Param("customerCode") long customerCode,
            @Param("market") String market,
            @Param("stkCode") String stkCode,
            @Param("stkName") String stkName,
            @Param("holdQty") long holdQty,
            @Param("availableQty") long availableQty
    );
}
