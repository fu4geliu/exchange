package com.example.customerservice.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
        Long credentialTypeId = findId("select id from dict_credential_type where type_code = ?", request.credentialTypeCode());
        Long accountCategoryId = findId("select id from account_category where category_code = ?", request.accountCategoryCode());
        Long accountStatusId = findId("select id from dict_account_status where status_code = 'NORMAL'");
        if (accountStatusId == null) {
            accountStatusId = findId("select id from dict_account_status where status_code = 'ACTIVE'");
        }
        if (accountStatusId == null) {
            accountStatusId = findId("select id from dict_account_status limit 1");
        }

        if (credentialTypeId == null || accountCategoryId == null || accountStatusId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dictionary data missing: credential/account category/status");
        }

        Integer credentialExists = jdbcTemplate.queryForObject(
                """
                select count(1) from customer
                where credential_type_id = ? and credential_number = ?
                """,
                Integer.class,
                credentialTypeId, request.credentialNumber());
        if (credentialExists != null && credentialExists > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该证件号码已开户，请勿重复开户");
        }

        String customerCode = "C" + System.currentTimeMillis();
        String username = generateUniqueUsername();
        String defaultPassword = "123456";

        jdbcTemplate.update(
                """
                insert into customer
                (customer_code, username, password, real_name, credential_type_id, credential_number, account_category_id, account_status_id)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                customerCode, username, defaultPassword, request.customerName(), credentialTypeId, request.credentialNumber(),
                accountCategoryId, accountStatusId);

        Long customerId = findId("select id from customer where customer_code = ?", customerCode);
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Create customer failed");
        }

        Long currencyId = findId("select id from dict_currency where currency_code = 'CNY'");
        if (currencyId == null) {
            currencyId = findId("select id from dict_currency limit 1");
        }
        if (currencyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency dictionary data missing");
        }

        BigDecimal initialDeposit = jdbcTemplate.queryForObject(
                "select initial_deposit_amount from account_category where id = ?",
                BigDecimal.class, accountCategoryId);
        if (initialDeposit == null) {
            initialDeposit = BigDecimal.ZERO;
        }

        String accountNumber = "FA" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        jdbcTemplate.update(
                """
                insert into customer_fund_account
                (customer_id, account_number, currency_id, balance, frozen_balance, total_asset)
                values (?, ?, ?, ?, 0, ?)
                """,
                customerId, accountNumber, currencyId, initialDeposit, initialDeposit);

        return Map.of(
                "customerId", customerId,
                "customerCode", customerCode,
                "fundAccountNumber", accountNumber,
                "createdAt", LocalDateTime.now().toString());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(@PathVariable("id") Long customerId) {
        Map<String, Object> customer = jdbcTemplate.query(
                """
                select c.id, c.customer_code, c.real_name, c.credential_number, dct.type_code, ac.category_code, cfa.account_number
                from customer c
                left join dict_credential_type dct on dct.id = c.credential_type_id
                left join account_category ac on ac.id = c.account_category_id
                left join customer_fund_account cfa on cfa.customer_id = c.id
                where c.id = ?
                """,
                rs -> rs.next() ? Map.of(
                        "customerId", rs.getLong("id"),
                        "customerCode", rs.getString("customer_code"),
                        "customerName", rs.getString("real_name"),
                        "credentialNumber", rs.getString("credential_number"),
                        "credentialTypeCode", rs.getString("type_code"),
                        "accountCategoryCode", rs.getString("category_code"),
                        "fundAccountNumber", rs.getString("account_number")) : null,
                customerId);

        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
        return customer;
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> updateCustomer(@PathVariable("id") Long customerId, @RequestBody CustomerUpdateRequest request) {
        int updated = jdbcTemplate.update(
                "update customer set real_name = ?, email = ?, phone_number = ? where id = ?",
                request.customerName(), request.email(), request.phoneNumber(), customerId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
        return Map.of("customerId", customerId, "updated", true);
    }

    private Long findId(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
    }

    private String generateUniqueUsername() {
        // 生成短且唯一性足够高的用户名，避免触发 customer.username 唯一约束冲突
        String username;
        do {
            int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
            username = "u_" + System.currentTimeMillis() + rand;
        } while (existsUsername(username));
        return username;
    }

    private boolean existsUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from customer where username = ?",
                Integer.class,
                username);
        return count != null && count > 0;
    }
}
