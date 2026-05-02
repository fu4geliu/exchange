package com.qianhua.customer.mapper;

import com.qianhua.customer.entity.SysAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysAccountMapper {

    SysAccount findByUsername(@Param("username") String username);

    int insert(SysAccount row);
}
