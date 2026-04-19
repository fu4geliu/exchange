package com.example.customerservice.customer;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final JdbcTemplate jdbcTemplate;

    public CustomerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> openCustomer(@RequestBody CustomerOpenRequest request) {
        if (!dictItemExists("ID_TYPE", request.credentialTypeCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "证件类型无效，请使用字典 ID_TYPE（如 00/01/02）");
        }
        if (!dictItemExists("CUACCT_CLS", request.accountCategoryCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资产账户类别无效，请使用字典 CUACCT_CLS（如 0/1/2/3）");
        }

        Integer dup = jdbcTemplate.queryForObject(
                "select count(1) from user_info where id_no = ?", Integer.class, request.credentialNumber());
        if (dup != null && dup > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该证件号码已开户，请勿重复开户");
        }

        long customerCode = allocateCustomerCode();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(
                    connection -> {
                        PreparedStatement ps = connection.prepareStatement(
                                """
                                insert into user_info
                                (customer_code, user_name, id_type, id_no, cuacct_cls, cuacct_status)
                                values (?, ?, ?, ?, ?, '0')
                                """,
                                Statement.RETURN_GENERATED_KEYS);
                        ps.setLong(1, customerCode);
                        ps.setString(2, request.customerName());
                        ps.setString(3, request.credentialTypeCode());
                        ps.setString(4, request.credentialNumber());
                        ps.setString(5, request.accountCategoryCode());
                        return ps;
                    },
                    keyHolder);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "客户代码冲突，请重试", ex);
        }

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Create user failed");
        }
        long userId = key.longValue();

        Map<String, Object> template = jdbcTemplate.query(
                """
                select cash_balance,
                       stk_code_1, stk_balance_1,
                       stk_code_2, stk_balance_2,
                       stk_code_3, stk_balance_3
                from customer_template
                where cuacct_cls = ?
                """,
                rs -> rs.next()
                        ? Map.of(
                                "cashBalance", rs.getBigDecimal("cash_balance"),
                                "stkCode1", rs.getString("stk_code_1"),
                                "stkBal1", rs.getLong("stk_balance_1"),
                                "stkCode2", rs.getString("stk_code_2"),
                                "stkBal2", rs.getLong("stk_balance_2"),
                                "stkCode3", rs.getString("stk_code_3"),
                                "stkBal3", rs.getLong("stk_balance_3"))
                        : null,
                request.accountCategoryCode());

        if (template == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户模板不存在，请检查资产账户类别");
        }

        BigDecimal cash = (BigDecimal) template.get("cashBalance");
        if (cash == null) {
            cash = BigDecimal.ZERO;
        }

        jdbcTemplate.update(
                """
                insert into customer_account
                (customer_code, currency, cash_balance, frozen_cash, total_assets)
                values (?, '0', ?, 0, ?)
                """,
                customerCode, cash, cash);

        seedPosition(customerCode, (String) template.get("stkCode1"), longOrZero(template.get("stkBal1")));
        seedPosition(customerCode, (String) template.get("stkCode2"), longOrZero(template.get("stkBal2")));
        seedPosition(customerCode, (String) template.get("stkCode3"), longOrZero(template.get("stkBal3")));

        return Map.of(
                "userId", userId,
                "customerCode", Long.toString(customerCode),
                "cuacctCls", request.accountCategoryCode(),
                "createdAt", LocalDateTime.now().toString());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(@PathVariable("id") Long userId) {
        Map<String, Object> customer = jdbcTemplate.query(
                """
                select u.user_id, u.customer_code, u.user_name, u.id_type, u.id_no, u.cuacct_cls, u.cuacct_status,
                       ca.account_id, ca.currency, ca.cash_balance, ca.frozen_cash, ca.total_assets
                from user_info u
                left join customer_account ca on ca.customer_code = u.customer_code
                where u.user_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", rs.getLong("user_id"));
                    m.put("customerCode", Long.toString(rs.getLong("customer_code")));
                    m.put("customerName", rs.getString("user_name"));
                    m.put("idType", rs.getString("id_type"));
                    m.put("idNo", rs.getString("id_no"));
                    m.put("cuacctCls", rs.getString("cuacct_cls"));
                    m.put("cuacctStatus", rs.getString("cuacct_status"));
                    m.put("accountId", rs.getObject("account_id") != null ? rs.getLong("account_id") : null);
                    m.put("currency", rs.getString("currency"));
                    m.put("cashBalance", rs.getBigDecimal("cash_balance"));
                    m.put("frozenCash", rs.getBigDecimal("frozen_cash"));
                    m.put("totalAssets", rs.getBigDecimal("total_assets"));
                    return m;
                },
                userId);

        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
        return customer;
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> updateCustomer(@PathVariable("id") Long userId, @RequestBody CustomerUpdateRequest request) {
        int updated = jdbcTemplate.update(
                "update user_info set user_name = ? where user_id = ?", request.customerName(), userId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
        return Map.of("userId", userId, "updated", true);
    }

    private static long longOrZero(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private void seedPosition(long customerCode, String stkCode, long holdQty) {
        if (stkCode == null || stkCode.isBlank() || holdQty <= 0) {
            return;
        }
        Map<String, Object> sec = jdbcTemplate.query(
                "select market, stk_name from security_info where stk_code = ?",
                rs -> rs.next()
                        ? Map.of("market", rs.getString("market"), "stkName", rs.getString("stk_name"))
                        : null,
                stkCode);
        if (sec == null) {
            return;
        }
        String market = (String) sec.get("market");
        String stkName = (String) sec.get("stkName");
        jdbcTemplate.update(
                """
                insert into customer_position
                (customer_code, market, stk_code, stk_name, hold_qty, available_qty, frozen_qty, avg_cost_price)
                values (?, ?, ?, ?, ?, ?, 0, 0)
                """,
                customerCode,
                market,
                stkCode,
                stkName,
                holdQty,
                holdQty);
    }

    private long allocateCustomerCode() {
        for (int i = 0; i < 32; i++) {
            long ts = System.currentTimeMillis();
            int r = ThreadLocalRandom.current().nextInt(0, 10_000);
            long code = Long.parseLong(ts + String.format("%04d", r));
            Integer c = jdbcTemplate.queryForObject(
                    "select count(1) from user_info where customer_code = ?", Integer.class, code);
            if (c == null || c == 0) {
                return code;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法分配客户代码，请重试");
    }

    private boolean dictItemExists(String dictCode, String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            return false;
        }
        Integer n = jdbcTemplate.queryForObject(
                "select count(1) from dict_item where dict_code = ? and item_code = ?",
                Integer.class,
                dictCode,
                itemCode);
        return n != null && n > 0;
    }
}
