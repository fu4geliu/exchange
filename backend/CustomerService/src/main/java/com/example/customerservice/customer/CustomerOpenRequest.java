package com.example.customerservice.customer;

public record CustomerOpenRequest(
        String customerName,
        String credentialTypeCode,
        String credentialNumber,
        String accountCategoryCode) {
}
