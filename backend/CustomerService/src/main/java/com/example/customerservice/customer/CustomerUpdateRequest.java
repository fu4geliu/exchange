package com.example.customerservice.customer;

public record CustomerUpdateRequest(
        String customerName,
        String email,
        String phoneNumber) {
}
