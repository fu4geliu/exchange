package com.qianhua.customer.mapper;

import com.qianhua.customer.entity.SecurityInfoRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SecurityInfoMapper {

    SecurityInfoRow findByStkCode(@Param("stkCode") String stkCode);
}
