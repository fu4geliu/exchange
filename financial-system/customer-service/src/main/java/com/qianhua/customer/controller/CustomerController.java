package com.qianhua.customer.controller;

import com.qianhua.common.dto.Result;
import com.qianhua.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("customer-service ok");
    }

    @PostMapping("/register")
    public ResponseEntity<Result<Map<String, Object>>> register(
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody Map<String, Object> payload
    ) {
        if (!"OPERATOR".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Result.<Map<String, Object>>fail("需要操作员权限"));
        }
        Result<Map<String, Object>> r = customerService.register(payload);
        if (!r.isSuccess()) {
            return ResponseEntity.status(400).body(r);
        }
        return ResponseEntity.ok(r);
    }

    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, Object>>> login(@RequestBody Map<String, Object> payload) {
        Result<Map<String, Object>> r = customerService.login(payload);
        if (!r.isSuccess()) {
            if ("权限不足".equals(r.getMessage())) {
                return ResponseEntity.status(403).body(r);
            }
            return ResponseEntity.status(401).body(r);
        }
        return ResponseEntity.ok(r);
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<Map<String, Object>>>> list(
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        if (!"OPERATOR".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Result.<List<Map<String, Object>>>fail("需要操作员权限"));
        }
        return ResponseEntity.ok(customerService.listCustomers());
    }

    /** 开户页下拉：证件类型、账户类别（来自 dict_item） */
    @GetMapping("/register-dicts")
    public ResponseEntity<Result<Map<String, Object>>> registerDicts(
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        if (!"OPERATOR".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Result.<Map<String, Object>>fail("需要操作员权限"));
        }
        return ResponseEntity.ok(customerService.registerDicts());
    }
}
