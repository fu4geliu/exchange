package com.qianhua.customer.mapper;

import com.qianhua.customer.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserInfoMapper {

    int countByIdNo(@Param("idNo") String idNo);

    int countByCustomerCode(@Param("customerCode") long customerCode);

    int insert(UserInfo row);

    List<UserInfo> selectAll();
}
