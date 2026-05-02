package com.qianhua.customer.entity;

import java.math.BigDecimal;

public class CustomerTemplate {
    private String cuacctCls;
    private BigDecimal cashBalance;
    private String stkCode1;
    private Long stkBalance1;
    private String stkCode2;
    private Long stkBalance2;
    private String stkCode3;
    private Long stkBalance3;

    public String getCuacctCls() {
        return cuacctCls;
    }

    public void setCuacctCls(String cuacctCls) {
        this.cuacctCls = cuacctCls;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public String getStkCode1() {
        return stkCode1;
    }

    public void setStkCode1(String stkCode1) {
        this.stkCode1 = stkCode1;
    }

    public Long getStkBalance1() {
        return stkBalance1;
    }

    public void setStkBalance1(Long stkBalance1) {
        this.stkBalance1 = stkBalance1;
    }

    public String getStkCode2() {
        return stkCode2;
    }

    public void setStkCode2(String stkCode2) {
        this.stkCode2 = stkCode2;
    }

    public Long getStkBalance2() {
        return stkBalance2;
    }

    public void setStkBalance2(Long stkBalance2) {
        this.stkBalance2 = stkBalance2;
    }

    public String getStkCode3() {
        return stkCode3;
    }

    public void setStkCode3(String stkCode3) {
        this.stkCode3 = stkCode3;
    }

    public Long getStkBalance3() {
        return stkBalance3;
    }

    public void setStkBalance3(Long stkBalance3) {
        this.stkBalance3 = stkBalance3;
    }
}
