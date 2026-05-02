package com.qianhua.customer.entity;

import java.time.LocalDateTime;

public class UserInfo {
    private Long userId;
    private Long customerCode;
    private String userName;
    private String idType;
    private String idNo;
    private String cuacctCls;
    private String cuacctStatus;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(Long customerCode) {
        this.customerCode = customerCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getCuacctCls() {
        return cuacctCls;
    }

    public void setCuacctCls(String cuacctCls) {
        this.cuacctCls = cuacctCls;
    }

    public String getCuacctStatus() {
        return cuacctStatus;
    }

    public void setCuacctStatus(String cuacctStatus) {
        this.cuacctStatus = cuacctStatus;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
