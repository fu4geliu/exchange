package com.qianhua.customer.mapper;

import com.qianhua.customer.entity.CustomerTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerTemplateMapper {

    CustomerTemplate findByCuacctCls(@Param("cuacctCls") String cuacctCls);
}
