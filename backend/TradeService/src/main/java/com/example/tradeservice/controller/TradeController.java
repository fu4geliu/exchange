package com.example.tradeservice.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final JdbcTemplate jdbcTemplate;

    public TradeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/order")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createOrder(@RequestBody OrderRequest request) {
        Long directionId = findId("select id from dict_trade_direction where direction_code = ?", request.directionCode());
        Long statusId = findId("select id from dict_order_status where status_code = 'TO_REPORT'");
        if (statusId == null) {
            statusId = findId("select id from dict_order_status where status_code = 'UNREPORTED'");
        }
        if (statusId == null) {
            statusId = findId("select id from dict_order_status limit 1");
        }
        Long securityId = findId("select id from security_info where security_code = ?", request.securityCode());
        Long marketId = securityId == null ? null : findId("select market_id from security_info where id = ?", securityId);
        String accountNumber = jdbcTemplate.query(
                "select account_number from customer_fund_account where id = ? and customer_id = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                request.capitalAccountId(), request.customerId());

        if (directionId == null || statusId == null || securityId == null || marketId == null || accountNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order input or dictionary data");
        }

        String orderCode = "O" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        jdbcTemplate.update(
                """
                insert into order_log
                (customer_id, account_number, security_id, market_id, order_code, direction_id, price, quantity, traded_quantity, status_id)
                values (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
                """,
                request.customerId(), accountNumber, securityId, marketId, orderCode, directionId, request.price(), request.quantity(), statusId);

        Long orderId = findId("select id from order_log where order_code = ?", orderCode);
        return Map.of("orderId", orderId, "orderCode", orderCode, "status", "TO_REPORT");
    }

    @PostMapping("/cancel")
    @Transactional
    public Map<String, Object> cancelOrder(@RequestBody CancelRequest request) {
        Map<String, Object> order = jdbcTemplate.query(
                "select id, customer_id, security_id, market_id, quantity, traded_quantity from order_log where id = ?",
                rs -> rs.next()
                        ? Map.of(
                                "id", rs.getLong("id"),
                                "customerId", rs.getLong("customer_id"),
                                "securityId", rs.getLong("security_id"),
                                "marketId", rs.getLong("market_id"),
                                "quantity", rs.getBigDecimal("quantity"),
                                "tradedQuantity", rs.getBigDecimal("traded_quantity"))
                        : null,
                request.orderId());
        if (order == null || !order.get("customerId").equals(request.customerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        Long canceledStatusId = findId("select id from dict_order_status where status_code = 'CANCELED'");
        if (canceledStatusId == null) {
            canceledStatusId = findId("select id from dict_order_status limit 1");
        }
        if (canceledStatusId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status dictionary missing");
        }

        jdbcTemplate.update("update order_log set status_id = ? where id = ?", canceledStatusId, request.orderId());

        BigDecimal quantity = (BigDecimal) order.get("quantity");
        BigDecimal tradedQuantity = (BigDecimal) order.get("tradedQuantity");
        BigDecimal cancelQuantity = quantity.subtract(tradedQuantity);
        String cancelCode = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        jdbcTemplate.update(
                """
                insert into cancel_log
                (order_id, customer_id, security_id, market_id, cancel_code, cancel_quantity, status)
                values (?, ?, ?, ?, ?, ?, 'SUCCESS')
                """,
                request.orderId(), request.customerId(), order.get("securityId"), order.get("marketId"), cancelCode, cancelQuantity);

        return Map.of("orderId", request.orderId(), "cancelCode", cancelCode, "status", "CANCELED");
    }

    @PostMapping("/execute")
    @Transactional
    public Map<String, Object> executeTrade(@RequestBody ExecuteRequest request) {
        Map<String, Object> order = jdbcTemplate.query(
                "select id, customer_id, security_id, market_id, direction_id, quantity, traded_quantity from order_log where id = ?",
                rs -> rs.next()
                        ? Map.of(
                                "id", rs.getLong("id"),
                                "customerId", rs.getLong("customer_id"),
                                "securityId", rs.getLong("security_id"),
                                "marketId", rs.getLong("market_id"),
                                "directionId", rs.getLong("direction_id"),
                                "quantity", rs.getBigDecimal("quantity"),
                                "tradedQuantity", rs.getBigDecimal("traded_quantity"))
                        : null,
                request.orderId());
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        BigDecimal quantity = (BigDecimal) order.get("quantity");
        BigDecimal tradedQuantity = (BigDecimal) order.get("tradedQuantity");
        BigDecimal remain = quantity.subtract(tradedQuantity);
        if (request.tradeQuantity().compareTo(remain) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trade quantity exceeds remain quantity");
        }

        String tradeCode = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        BigDecimal amount = request.tradePrice().multiply(request.tradeQuantity());
        jdbcTemplate.update(
                """
                insert into trade_log
                (order_id, customer_id, security_id, market_id, trade_code, direction_id, trade_price, trade_quantity, trade_amount, commission_fee, stamp_tax)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                request.orderId(), order.get("customerId"), order.get("securityId"), order.get("marketId"), tradeCode,
                order.get("directionId"), request.tradePrice(), request.tradeQuantity(), amount);

        Long tradeId = findId("select id from trade_log where trade_code = ?", tradeCode);
        BigDecimal newTradedQuantity = tradedQuantity.add(request.tradeQuantity());

        Long filledStatusId = findId("select id from dict_order_status where status_code = 'FILLED'");
        Long partialStatusId = findId("select id from dict_order_status where status_code = 'PART_FILL'");
        Long fallbackStatusId = findId("select id from dict_order_status limit 1");
        Long nextStatusId = newTradedQuantity.compareTo(quantity) >= 0
                ? (filledStatusId != null ? filledStatusId : fallbackStatusId)
                : (partialStatusId != null ? partialStatusId : fallbackStatusId);

        jdbcTemplate.update(
                "update order_log set traded_quantity = ?, status_id = ? where id = ?",
                newTradedQuantity, nextStatusId, request.orderId());

        return Map.of(
                "tradeId", tradeId,
                "tradeCode", tradeCode,
                "orderId", request.orderId(),
                "tradedQuantity", newTradedQuantity,
                "updatedAt", LocalDateTime.now().toString());
    }

    @GetMapping("/position/{customerId}/{capitalAccountId}")
    public List<Map<String, Object>> queryPosition(
            @PathVariable Long customerId,
            @PathVariable Long capitalAccountId) {
        // capitalAccountId 用于接口兼容，这里由 customer_id 关联持仓。
        return jdbcTemplate.queryForList(
                """
                select cp.id, si.security_code, si.security_name, cp.total_quantity, cp.available_quantity, cp.frozen_quantity, cp.cost_price
                from customer_position cp
                left join security_info si on si.id = cp.security_id
                where cp.customer_id = ?
                order by cp.updated_at desc
                """,
                customerId);
    }

    @GetMapping("/orders/{customerId}")
    public List<Map<String, Object>> queryOrders(
            @PathVariable Long customerId,
            @RequestParam(required = false) Long capitalAccountId,
            @RequestParam(required = false) String securityAccountId) {
        // capitalAccountId/securityAccountId 暂未参与过滤，保留参数以便前端对齐。
        return jdbcTemplate.queryForList(
                """
                select ol.id, ol.order_code, si.security_code, ol.price, ol.quantity, ol.traded_quantity, dos.status_code, ol.order_time
                from order_log ol
                left join security_info si on si.id = ol.security_id
                left join dict_order_status dos on dos.id = ol.status_id
                where ol.customer_id = ?
                order by ol.order_time desc
                """,
                customerId);
    }

    @GetMapping("/trades/{customerId}")
    public List<Map<String, Object>> queryTrades(
            @PathVariable Long customerId,
            @RequestParam(required = false) Long capitalAccountId,
            @RequestParam(required = false) String securityAccountId) {
        // capitalAccountId/securityAccountId 暂未参与过滤，保留参数以便前端对齐。
        return jdbcTemplate.queryForList(
                """
                select tl.id, tl.trade_code, tl.order_id, si.security_code, tl.trade_price, tl.trade_quantity, tl.trade_amount, tl.trade_time
                from trade_log tl
                left join security_info si on si.id = tl.security_id
                where tl.customer_id = ?
                order by tl.trade_time desc
                """,
                customerId);
    }

    private Long findId(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
    }

    public record OrderRequest(
            Long customerId,
            Long capitalAccountId,
            String securityCode,
            BigDecimal price,
            BigDecimal quantity,
            String directionCode) {
    }

    public record CancelRequest(
            Long orderId,
            Long customerId) {
    }

    public record ExecuteRequest(
            Long orderId,
            BigDecimal tradePrice,
            BigDecimal tradeQuantity) {
    }
}
