package com.qianhua.customer.support;

import com.qianhua.customer.entity.SysAccount;
import com.qianhua.customer.mapper.SysAccountMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 若库中无操作员账号，则创建默认 operator / 123456，便于首次启动联调。
 */
@Component
public class OperatorDataInitializer {

    private static final String DEFAULT_OPERATOR = "operator";
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysAccountMapper sysAccountMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public OperatorDataInitializer(SysAccountMapper sysAccountMapper, BCryptPasswordEncoder passwordEncoder) {
        this.sysAccountMapper = sysAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (sysAccountMapper.findByUsername(DEFAULT_OPERATOR) != null) {
            return;
        }
        SysAccount a = new SysAccount();
        a.setUsername(DEFAULT_OPERATOR);
        a.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        a.setRole("OPERATOR");
        a.setRefId(null);
        a.setStatus("0");
        sysAccountMapper.insert(a);
    }
}
