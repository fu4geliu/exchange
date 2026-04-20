package com.example.tradeservice.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
        if ("B".equals(trdId)) {
            freezeBuyFunds(customerCode, orderAmount);
        } else {
            freezeSellShares(customerCode, market, request.securityCode(), qtyLong);
        }

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
                select order_id, customer_code, market, stk_code, trd_id, order_price, order_qty, matched_qty, order_status, is_withdraw
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
                                "orderPrice", rs.getBigDecimal("order_price"),
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

        Map<String, Object> release = releaseFreezeOnCancel(order);
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", request.orderId());
        response.put("orderStatus", "6");
        response.put("isWithdraw", "T");
        response.putAll(release);
        return response;
    }

    @PostMapping("/execute")
    @Transactional
    public Map<String, Object> executeTrade(@RequestBody ExecuteRequest request) {
        Map<String, Object> order = jdbcTemplate.query(
                """
                select order_id, customer_code, market, stk_code, trd_id, order_price, order_qty, matched_qty
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
                                "orderPrice", rs.getBigDecimal("order_price"),
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
        FeeBreakdown fee = calculateFees(order, tradeAmount);

        KeyHolder tradeKeys = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            """
                            insert into trade_info
                            (order_id, customer_code, market, stk_code, trd_id, trade_price, trade_qty, trade_amount,
                             commission_fee, stamp_tax_fee, total_fee)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    ps.setBigDecimal(9, fee.commissionFee());
                    ps.setBigDecimal(10, fee.stampTaxFee());
                    ps.setBigDecimal(11, fee.totalFee());
                    return ps;
                },
                tradeKeys);

        Map<String, Object> settlement =
                settleOnExecute(order, tradeQty, request.tradePrice(), tradeAmount, fee.totalFee());

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

        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("tradeId", tradeKey != null ? tradeKey.longValue() : 0L);
        response.put("orderId", request.orderId());
        response.put("matchedQty", newMatched);
        response.put("orderStatus", nextStatus);
        response.put("updatedAt", LocalDateTime.now().toString());
        response.put("commissionFee", fee.commissionFee());
        response.put("stampTaxFee", fee.stampTaxFee());
        response.put("totalFee", fee.totalFee());
        response.putAll(settlement);
        return response;
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

    private void freezeBuyFunds(long customerCode, BigDecimal orderAmount) {
        Map<String, Object> account = jdbcTemplate.query(
                """
                select cash_balance, frozen_cash
                from customer_account
                where customer_code = ?
                """,
                rs -> rs.next()
                        ? Map.of(
                                "cashBalance", rs.getBigDecimal("cash_balance"),
                                "frozenCash", rs.getBigDecimal("frozen_cash"))
                        : null,
                customerCode);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资金账户不存在");
        }
        BigDecimal cashBalance = (BigDecimal) account.get("cashBalance");
        BigDecimal frozenCash = (BigDecimal) account.get("frozenCash");
        if (cashBalance == null) {
            cashBalance = BigDecimal.ZERO;
        }
        if (frozenCash == null) {
            frozenCash = BigDecimal.ZERO;
        }
        BigDecimal available = cashBalance.subtract(frozenCash);
        if (available.compareTo(orderAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "可用资金不足，无法买入下单");
        }
        jdbcTemplate.update(
                """
                update customer_account
                set frozen_cash = frozen_cash + ?
                where customer_code = ?
                """,
                orderAmount,
                customerCode);
    }

    private void freezeSellShares(long customerCode, String market, String stkCode, long orderQty) {
        Map<String, Object> pos = jdbcTemplate.query(
                """
                select available_qty
                from customer_position
                where customer_code = ? and market = ? and stk_code = ?
                """,
                rs -> rs.next() ? Map.of("availableQty", rs.getLong("available_qty")) : null,
                customerCode,
                market,
                stkCode);
        if (pos == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到该证券持仓，无法卖出下单");
        }
        long availableQty = (Long) pos.get("availableQty");
        if (availableQty < orderQty) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "可卖数量不足，无法卖出下单");
        }
        jdbcTemplate.update(
                """
                update customer_position
                set available_qty = available_qty - ?,
                    frozen_qty = frozen_qty + ?
                where customer_code = ? and market = ? and stk_code = ?
                """,
                orderQty,
                orderQty,
                customerCode,
                market,
                stkCode);
    }

    private Map<String, Object> releaseFreezeOnCancel(Map<String, Object> order) {
        String trdId = (String) order.get("trdId");
        long customerCode = (Long) order.get("customerCode");
        long orderQty = (Long) order.get("orderQty");
        long matchedQty = (Long) order.get("matchedQty");
        long remainQty = orderQty - matchedQty;
        LinkedHashMap<String, Object> released = new LinkedHashMap<>();
        released.put("releasedQty", remainQty > 0 ? remainQty : 0L);
        released.put("releasedFunds", BigDecimal.ZERO);
        if (remainQty <= 0) {
            return released;
        }

        if ("B".equals(trdId)) {
            BigDecimal orderPrice = (BigDecimal) order.get("orderPrice");
            BigDecimal releaseFunds =
                    orderPrice.multiply(BigDecimal.valueOf(remainQty)).setScale(2, RoundingMode.HALF_UP);
            jdbcTemplate.update(
                    """
                    update customer_account
                    set frozen_cash = case
                        when frozen_cash >= ? then frozen_cash - ?
                        else 0
                    end
                    where customer_code = ?
                    """,
                    releaseFunds,
                    releaseFunds,
                    customerCode);
            released.put("releasedFunds", releaseFunds);
            return released;
        }

        String market = (String) order.get("market");
        String stkCode = (String) order.get("stkCode");
        jdbcTemplate.update(
                """
                update customer_position
                set available_qty = available_qty + ?,
                    frozen_qty = case
                        when frozen_qty >= ? then frozen_qty - ?
                        else 0
                    end
                where customer_code = ? and market = ? and stk_code = ?
                """,
                remainQty,
                remainQty,
                remainQty,
                customerCode,
                market,
                stkCode);
        return released;
    }

    private Map<String, Object> settleOnExecute(
            Map<String, Object> order, long tradeQty, BigDecimal tradePrice, BigDecimal tradeAmount, BigDecimal totalFee) {
        String trdId = (String) order.get("trdId");
        long customerCode = (Long) order.get("customerCode");
        String market = (String) order.get("market");
        String stkCode = (String) order.get("stkCode");
        LinkedHashMap<String, Object> settlement = new LinkedHashMap<>();

        if ("B".equals(trdId)) {
            BigDecimal orderPrice = (BigDecimal) order.get("orderPrice");
            BigDecimal unfreezeFunds =
                    orderPrice.multiply(BigDecimal.valueOf(tradeQty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal actualDeductFunds = tradeAmount.add(totalFee).setScale(2, RoundingMode.HALF_UP);
            BigDecimal refundFunds = unfreezeFunds.subtract(actualDeductFunds);
            jdbcTemplate.update(
                    """
                    update customer_account
                    set frozen_cash = case
                            when frozen_cash >= ? then frozen_cash - ?
                            else 0
                        end,
                        cash_balance = cash_balance - ?
                    where customer_code = ?
                    """,
                    unfreezeFunds,
                    unfreezeFunds,
                    actualDeductFunds,
                    customerCode);

            settlement.put("settlementType", "BUY_EXECUTE");
            settlement.put("unfreezeFunds", unfreezeFunds);
            settlement.put("actualDeductFunds", actualDeductFunds);
            settlement.put("refundFunds", refundFunds.compareTo(BigDecimal.ZERO) > 0 ? refundFunds : BigDecimal.ZERO);
            settlement.put("extraDeductFunds", refundFunds.compareTo(BigDecimal.ZERO) < 0 ? refundFunds.negate() : BigDecimal.ZERO);

            Map<String, Object> pos = jdbcTemplate.query(
                    """
                    select hold_qty, available_qty, avg_cost_price
                    from customer_position
                    where customer_code = ? and market = ? and stk_code = ?
                    """,
                    rs -> rs.next()
                            ? Map.of(
                                    "holdQty", rs.getLong("hold_qty"),
                                    "availableQty", rs.getLong("available_qty"),
                                    "avgCostPrice", rs.getBigDecimal("avg_cost_price"))
                            : null,
                    customerCode,
                    market,
                    stkCode);

            if (pos == null) {
                String stkName = jdbcTemplate.query(
                        "select stk_name from security_info where market = ? and stk_code = ?",
                        rs -> rs.next() ? rs.getString(1) : stkCode,
                        market,
                        stkCode);
                jdbcTemplate.update(
                        """
                        insert into customer_position
                        (customer_code, market, stk_code, stk_name, hold_qty, available_qty, frozen_qty, avg_cost_price)
                        values (?, ?, ?, ?, ?, ?, 0, ?)
                        """,
                        customerCode,
                        market,
                        stkCode,
                        stkName,
                        tradeQty,
                        tradeQty,
                        tradePrice.setScale(4, RoundingMode.HALF_UP));
                settlement.put("positionDelta", tradeQty);
                return settlement;
            }

            long oldHoldQty = (Long) pos.get("holdQty");
            long oldAvailableQty = (Long) pos.get("availableQty");
            BigDecimal oldAvgCost = (BigDecimal) pos.get("avgCostPrice");
            if (oldAvgCost == null) {
                oldAvgCost = BigDecimal.ZERO;
            }

            long newHoldQty = oldHoldQty + tradeQty;
            long newAvailableQty = oldAvailableQty + tradeQty;
            BigDecimal totalCost = oldAvgCost.multiply(BigDecimal.valueOf(oldHoldQty)).add(tradeAmount);
            BigDecimal newAvgCost = totalCost.divide(BigDecimal.valueOf(newHoldQty), 4, RoundingMode.HALF_UP);

            jdbcTemplate.update(
                    """
                    update customer_position
                    set hold_qty = ?, available_qty = ?, avg_cost_price = ?
                    where customer_code = ? and market = ? and stk_code = ?
                    """,
                    newHoldQty,
                    newAvailableQty,
                    newAvgCost,
                    customerCode,
                    market,
                    stkCode);
            settlement.put("positionDelta", tradeQty);
            return settlement;
        }

        int updatedPos = jdbcTemplate.update(
                """
                update customer_position
                set hold_qty = hold_qty - ?,
                    frozen_qty = case
                        when frozen_qty >= ? then frozen_qty - ?
                        else 0
                    end
                where customer_code = ? and market = ? and stk_code = ? and hold_qty >= ?
                """,
                tradeQty,
                tradeQty,
                tradeQty,
                customerCode,
                market,
                stkCode,
                tradeQty);
        if (updatedPos == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "持仓不足，无法完成卖出成交");
        }

        jdbcTemplate.update(
                """
                update customer_account
                set cash_balance = cash_balance + ?
                where customer_code = ?
                """,
                tradeAmount.subtract(totalFee).setScale(2, RoundingMode.HALF_UP),
                customerCode);
        settlement.put("settlementType", "SELL_EXECUTE");
        settlement.put("unfreezeQty", tradeQty);
        settlement.put("positionDelta", -tradeQty);
        settlement.put("cashIncreased", tradeAmount.subtract(totalFee).setScale(2, RoundingMode.HALF_UP));
        return settlement;
    }

    private FeeBreakdown calculateFees(Map<String, Object> order, BigDecimal tradeAmount) {
        long customerCode = (Long) order.get("customerCode");
        String market = (String) order.get("market");
        String stkCode = (String) order.get("stkCode");
        String trdId = (String) order.get("trdId");

        Map<String, Object> meta = jdbcTemplate.query(
                """
                select u.cuacct_cls, s.stk_cls, s.stamp_tax_rate
                from user_info u
                join security_info s on s.market = ? and s.stk_code = ?
                where u.customer_code = ?
                """,
                rs -> rs.next()
                        ? Map.of(
                                "cuacctCls", rs.getString("cuacct_cls"),
                                "stkCls", rs.getString("stk_cls"),
                                "stampTaxRate", rs.getBigDecimal("stamp_tax_rate"))
                        : null,
                market,
                stkCode,
                customerCode);
        if (meta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "佣金/印花税计算所需元数据缺失");
        }

        BigDecimal commissionRate = jdbcTemplate.query(
                """
                select commission_rate
                from commission_rate
                where cuacct_cls = ? and market = ? and stk_cls = ?
                """,
                rs -> rs.next() ? rs.getBigDecimal(1) : null,
                meta.get("cuacctCls"),
                market,
                meta.get("stkCls"));
        if (commissionRate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "佣金费率未配置");
        }

        BigDecimal commissionFee = tradeAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal stampTaxRate = (BigDecimal) meta.get("stampTaxRate");
        if (stampTaxRate == null) {
            stampTaxRate = BigDecimal.ZERO;
        }
        BigDecimal stampTaxFee = "S".equals(trdId)
                ? tradeAmount.multiply(stampTaxRate).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFee = commissionFee.add(stampTaxFee).setScale(2, RoundingMode.HALF_UP);
        return new FeeBreakdown(commissionFee, stampTaxFee, totalFee);
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

    private record FeeBreakdown(BigDecimal commissionFee, BigDecimal stampTaxFee, BigDecimal totalFee) {}
}
