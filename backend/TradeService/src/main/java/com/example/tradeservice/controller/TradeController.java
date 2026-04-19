package com.example.tradeservice.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
        long customerCode = parseCustomerCode(request.customerCode());
        String trdId = resolveTrdId(request.directionCode());
        if (trdId == null || !dictItemExists("TRD_ID", trdId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "买卖方向无效（使用 BUY/SELL 或 B/S）");
        }

        Map<String, Object> sec = jdbcTemplate.query(
                "select market from security_info where stk_code = ?",
                rs -> rs.next() ? Map.of("market", rs.getString("market")) : null,
                request.securityCode());
        if (sec == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "证券代码不存在");
        }
        String market = (String) sec.get("market");

        Integer exists = jdbcTemplate.queryForObject(
                "select count(1) from user_info where customer_code = ?", Integer.class, customerCode);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户代码不存在");
        }

        long qtyLong = parsePositiveIntegerQty(request.quantity(), "委托数量");

        BigDecimal orderAmount =
                request.price().multiply(request.quantity()).setScale(2, RoundingMode.HALF_UP);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        long finalCustomerCode = customerCode;
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            """
                            insert into order_info
                            (customer_code, market, stk_code, trd_id, order_price, order_qty, matched_qty, order_amount, order_status, is_withdraw)
                            values (?, ?, ?, ?, ?, ?, 0, ?, 'A', 'F')
                            """,
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, finalCustomerCode);
                    ps.setString(2, market);
                    ps.setString(3, request.securityCode());
                    ps.setString(4, trdId);
                    ps.setBigDecimal(5, request.price());
                    ps.setLong(6, qtyLong);
                    ps.setBigDecimal(7, orderAmount);
                    return ps;
                },
                keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建委托失败");
        }

        return Map.of(
                "orderId", key.longValue(),
                "orderStatus", "A",
                "createdAt", LocalDateTime.now().toString());
    }

    @PostMapping("/cancel")
    @Transactional
    public Map<String, Object> cancelOrder(@RequestBody CancelRequest request) {
        long customerCode = parseCustomerCode(request.customerCode());
        Map<String, Object> order = jdbcTemplate.query(
                """
                select order_id, customer_code, order_qty, matched_qty, order_status, is_withdraw
                from order_info
                where order_id = ?
                """,
                rs -> rs.next()
                        ? Map.of(
                                "orderId", rs.getLong("order_id"),
                                "customerCode", rs.getLong("customer_code"),
                                "orderQty", rs.getLong("order_qty"),
                                "matchedQty", rs.getLong("matched_qty"),
                                "orderStatus", rs.getString("order_status"),
                                "isWithdraw", rs.getString("is_withdraw"))
                        : null,
                request.orderId());

        if (order == null || !((Long) order.get("customerCode")).equals(customerCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "委托不存在");
        }

        String isWithdraw = (String) order.get("isWithdraw");
        String status = (String) order.get("orderStatus");
        if (!"F".equals(isWithdraw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该委托不可重复撤单");
        }
        if ("6".equals(status) || "8".equals(status) || "9".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前委托状态不可撤单");
        }

        jdbcTemplate.update(
                """
                update order_info
                set order_status = '6', is_withdraw = 'T'
                where order_id = ?
                """,
                request.orderId());

        jdbcTemplate.update(
                """
                insert into withdraw_info (order_id, customer_code, withdraw_reason)
                values (?, ?, ?)
                """,
                request.orderId(),
                customerCode,
                "用户撤单");

        return Map.of("orderId", request.orderId(), "orderStatus", "6", "isWithdraw", "T");
    }

    @PostMapping("/execute")
    @Transactional
    public Map<String, Object> executeTrade(@RequestBody ExecuteRequest request) {
        Map<String, Object> order = jdbcTemplate.query(
                """
                select order_id, customer_code, market, stk_code, trd_id, order_qty, matched_qty
                from order_info
                where order_id = ?
                """,
                rs -> rs.next()
                        ? Map.of(
                                "orderId", rs.getLong("order_id"),
                                "customerCode", rs.getLong("customer_code"),
                                "market", rs.getString("market"),
                                "stkCode", rs.getString("stk_code"),
                                "trdId", rs.getString("trd_id"),
                                "orderQty", rs.getLong("order_qty"),
                                "matchedQty", rs.getLong("matched_qty"))
                        : null,
                request.orderId());

        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "委托不存在");
        }

        long orderQty = (Long) order.get("orderQty");
        long matchedQty = (Long) order.get("matchedQty");
        long remain = orderQty - matchedQty;
        long tradeQty = parsePositiveIntegerQty(request.tradeQuantity(), "成交数量");
        if (tradeQty <= 0 || tradeQty > remain) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成交数量无效或超过剩余数量");
        }

        BigDecimal tradeAmount =
                request.tradePrice().multiply(BigDecimal.valueOf(tradeQty)).setScale(2, RoundingMode.HALF_UP);

        KeyHolder tradeKeys = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            """
                            insert into trade_info
                            (order_id, customer_code, market, stk_code, trd_id, trade_price, trade_qty, trade_amount,
                             commission_fee, stamp_tax_fee, total_fee)
                            values (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0)
                            """,
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, request.orderId());
                    ps.setLong(2, (Long) order.get("customerCode"));
                    ps.setString(3, (String) order.get("market"));
                    ps.setString(4, (String) order.get("stkCode"));
                    ps.setString(5, (String) order.get("trdId"));
                    ps.setBigDecimal(6, request.tradePrice());
                    ps.setLong(7, tradeQty);
                    ps.setBigDecimal(8, tradeAmount);
                    return ps;
                },
                tradeKeys);

        Number tradeKey = tradeKeys.getKey();
        long newMatched = matchedQty + tradeQty;
        String nextStatus = newMatched >= orderQty ? "8" : "7";

        jdbcTemplate.update(
                """
                update order_info
                set matched_qty = ?, order_status = ?
                where order_id = ?
                """,
                newMatched,
                nextStatus,
                request.orderId());

        return Map.of(
                "tradeId", tradeKey != null ? tradeKey.longValue() : 0L,
                "orderId", request.orderId(),
                "matchedQty", newMatched,
                "orderStatus", nextStatus,
                "updatedAt", LocalDateTime.now().toString());
    }

    @GetMapping("/position/{customerCode}")
    public List<Map<String, Object>> queryPosition(@PathVariable String customerCode) {
        return jdbcTemplate.queryForList(
                """
                select position_id, market, stk_code, stk_name, hold_qty, available_qty, frozen_qty, avg_cost_price, updated_time
                from customer_position
                where customer_code = ?
                order by updated_time desc
                """,
                parseCustomerCode(customerCode));
    }

    @GetMapping("/orders/{customerCode}")
    public List<Map<String, Object>> queryOrders(
            @PathVariable String customerCode,
            @RequestParam(required = false) Long capitalAccountId,
            @RequestParam(required = false) String securityAccountId) {
        return jdbcTemplate.queryForList(
                """
                select order_id, market, stk_code, trd_id, order_price, order_qty, matched_qty, order_amount,
                       order_status, is_withdraw, entrust_time
                from order_info
                where customer_code = ?
                order by entrust_time desc
                """,
                parseCustomerCode(customerCode));
    }

    @GetMapping("/trades/{customerCode}")
    public List<Map<String, Object>> queryTrades(
            @PathVariable String customerCode,
            @RequestParam(required = false) Long capitalAccountId,
            @RequestParam(required = false) String securityAccountId) {
        return jdbcTemplate.queryForList(
                """
                select trade_id, order_id, market, stk_code, trd_id, trade_price, trade_qty, trade_amount,
                       commission_fee, stamp_tax_fee, total_fee, trade_time
                from trade_info
                where customer_code = ?
                order by trade_time desc
                """,
                parseCustomerCode(customerCode));
    }

    private static long parseCustomerCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户代码不能为空");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户代码格式无效", ex);
        }
    }

    private static long parsePositiveIntegerQty(BigDecimal qty, String label) {
        if (qty == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不能为空");
        }
        try {
            long v = qty.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
            if (v <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "须为正整数");
            }
            return v;
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "须为正整数", ex);
        }
    }

    private boolean dictItemExists(String dictCode, String itemCode) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(1) from dict_item where dict_code = ? and item_code = ?",
                Integer.class,
                dictCode,
                itemCode);
        return n != null && n > 0;
    }

    /** BUY -> B，SELL -> S；也可直接传 B/S */
    private static String resolveTrdId(String directionCode) {
        if (directionCode == null) {
            return null;
        }
        String u = directionCode.trim().toUpperCase();
        return switch (u) {
            case "BUY", "B" -> "B";
            case "SELL", "S" -> "S";
            default -> null;
        };
    }

    public record OrderRequest(
            String customerCode, String securityCode, BigDecimal price, BigDecimal quantity, String directionCode) {}

    public record CancelRequest(Long orderId, String customerCode) {}

    public record ExecuteRequest(Long orderId, BigDecimal tradePrice, BigDecimal tradeQuantity) {}
}
