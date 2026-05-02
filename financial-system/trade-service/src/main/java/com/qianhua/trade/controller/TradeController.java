package com.qianhua.trade.controller;

import com.qianhua.common.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/trade")
public class TradeController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("trade-service ok");
    }

    @PostMapping("/order")
    public Result<Map<String, Object>> order(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", System.currentTimeMillis());
        data.put("status", "ORDER_ACCEPTED");
        data.put("input", payload);
        return Result.ok(data);
    }

    @PostMapping("/withdraw")
    public Result<Map<String, Object>> withdraw(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = new HashMap<>();
        data.put("withdrawId", System.currentTimeMillis());
        data.put("status", "WITHDRAW_ACCEPTED");
        data.put("input", payload);
        return Result.ok(data);
    }

    @PostMapping("/deal")
    public Result<Map<String, Object>> deal(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", System.currentTimeMillis());
        data.put("status", "DEAL_ACCEPTED");
        data.put("input", payload);
        return Result.ok(data);
    }
}
