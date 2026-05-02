package com.qianhua.customer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface CustomerAccountMapper {

    int insert(
            @Param("customerCode") long customerCode,
            @Param("currency") String currency,
            @Param("cashBalance") BigDecimal cashBalance,
            @Param("totalAssets") BigDecimal totalAssets
    );
}
