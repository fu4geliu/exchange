package com.qianhua.customer.service;

import com.qianhua.common.dto.Result;
import com.qianhua.common.utils.JwtUtil;
import com.qianhua.customer.entity.CustomerTemplate;
import com.qianhua.customer.entity.SecurityInfoRow;
import com.qianhua.customer.entity.SysAccount;
import com.qianhua.customer.entity.UserInfo;
import com.qianhua.customer.mapper.CustomerAccountMapper;
import com.qianhua.customer.mapper.CustomerPositionMapper;
import com.qianhua.customer.mapper.CustomerTemplateMapper;
import com.qianhua.customer.mapper.DictItemMapper;
import com.qianhua.customer.mapper.SecurityInfoMapper;
import com.qianhua.customer.mapper.SysAccountMapper;
import com.qianhua.customer.mapper.UserInfoMapper;
import com.qianhua.customer.support.RegisterPayloadValidator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CustomerService {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final String ROLE_OPERATOR = "OPERATOR";
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ACCOUNT_NORMAL = "0";

    private final UserInfoMapper userInfoMapper;
    private final SysAccountMapper sysAccountMapper;
    private final CustomerTemplateMapper customerTemplateMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerPositionMapper customerPositionMapper;
    private final SecurityInfoMapper securityInfoMapper;
    private final DictItemMapper dictItemMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomerService(
            UserInfoMapper userInfoMapper,
            SysAccountMapper sysAccountMapper,
            CustomerTemplateMapper customerTemplateMapper,
            CustomerAccountMapper customerAccountMapper,
            CustomerPositionMapper customerPositionMapper,
            SecurityInfoMapper securityInfoMapper,
            DictItemMapper dictItemMapper,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userInfoMapper = userInfoMapper;
        this.sysAccountMapper = sysAccountMapper;
        this.customerTemplateMapper = customerTemplateMapper;
        this.customerAccountMapper = customerAccountMapper;
        this.customerPositionMapper = customerPositionMapper;
        this.securityInfoMapper = securityInfoMapper;
        this.dictItemMapper = dictItemMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Result<Map<String, Object>> registerDicts() {
        Map<String, Object> data = new HashMap<>();
        data.put("idTypes", dictItemMapper.listByDictCode("ID_TYPE"));
        data.put("cuacctClasses", dictItemMapper.listByDictCode("CUACCT_CLS"));
        return Result.ok(data);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> register(Map<String, Object> payload) {
        String userName = str(payload, "userName");
        String idType = str(payload, "idType");
        String idNo = str(payload, "idNo");
        String cuacctCls = firstNonBlank(str(payload, "cuacctCls"), str(payload, "cuacct_cls"));
        String pwdField = str(payload, "password");
        String rawPwd = StringUtils.hasText(pwdField) ? pwdField : DEFAULT_PASSWORD;

        if (!StringUtils.hasText(userName) || !StringUtils.hasText(idType)
                || !StringUtils.hasText(idNo) || !StringUtils.hasText(cuacctCls)) {
            return Result.fail("缺少必要字段：姓名、证件类型、证件号、账户类别");
        }

        String err = RegisterPayloadValidator.validateUserName(userName);
        if (err != null) {
            return Result.fail(err);
        }
        if (dictItemMapper.countItem("ID_TYPE", idType) == 0) {
            return Result.fail("证件类型无效，请从字典中选择");
        }
        if (dictItemMapper.countItem("CUACCT_CLS", cuacctCls) == 0) {
            return Result.fail("账户类别无效，请从字典中选择");
        }
        err = RegisterPayloadValidator.validateIdNo(idType, idNo);
        if (err != null) {
            return Result.fail(err);
        }
        err = RegisterPayloadValidator.validatePassword(rawPwd);
        if (err != null) {
            return Result.fail(err);
        }

        userName = userName.trim();
        idNo = idNo.trim();

        if (userInfoMapper.countByIdNo(idNo) > 0) {
            return Result.fail("证件号已存在，不能重复开户");
        }

        CustomerTemplate template = customerTemplateMapper.findByCuacctCls(cuacctCls);
        if (template == null) {
            return Result.fail("不存在的账户类别: " + cuacctCls);
        }

        long customerCode = generateUniqueCustomerCode();

        UserInfo user = new UserInfo();
        user.setCustomerCode(customerCode);
        user.setUserName(userName);
        user.setIdType(idType);
        user.setIdNo(idNo);
        user.setCuacctCls(cuacctCls);
        user.setCuacctStatus("0");
        userInfoMapper.insert(user);

        SysAccount account = new SysAccount();
        account.setUsername(String.valueOf(customerCode));
        account.setPassword(passwordEncoder.encode(rawPwd));
        account.setRole(ROLE_CUSTOMER);
        account.setRefId(customerCode);
        account.setStatus(ACCOUNT_NORMAL);
        sysAccountMapper.insert(account);

        BigDecimal cash = template.getCashBalance() == null ? BigDecimal.ZERO : template.getCashBalance();
        BigDecimal totalAssets = cash;
        customerAccountMapper.insert(customerCode, "0", cash, totalAssets);

        addPositionIfAny(customerCode, template.getStkCode1(), template.getStkBalance1());
        addPositionIfAny(customerCode, template.getStkCode2(), template.getStkBalance2());
        addPositionIfAny(customerCode, template.getStkCode3(), template.getStkBalance3());

        Map<String, Object> data = new HashMap<>();
        data.put("customerCode", customerCode);
        data.put("userName", userName);
        data.put("idNo", idNo);
        data.put("cuacctCls", cuacctCls);
        data.put("status", "REGISTERED");
        return Result.ok(data);
    }

    public Result<Map<String, Object>> login(Map<String, Object> payload) {
        String accountId = firstNonBlank(str(payload, "accountId"), str(payload, "username"));
        String password = str(payload, "password");
        if (!StringUtils.hasText(accountId) || !StringUtils.hasText(password)) {
            return Result.fail("缺少 accountId 或 password");
        }

        SysAccount account = sysAccountMapper.findByUsername(accountId.trim());
        if (account == null) {
            return Result.fail("用户名或密码错误");
        }
        if (!ACCOUNT_NORMAL.equals(account.getStatus())) {
            return Result.fail("账户已禁用");
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        if (!ROLE_OPERATOR.equals(account.getRole())) {
            return Result.fail("权限不足");
        }

        String refId = account.getRefId() == null ? "" : String.valueOf(account.getRefId());
        String token = JwtUtil.issueToken(account.getUsername(), account.getRole(), refId);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", account.getRole());
        data.put("status", "LOGIN_OK");
        return Result.ok(data);
    }

    public Result<List<Map<String, Object>>> listCustomers() {
        List<UserInfo> rows = userInfoMapper.selectAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserInfo u : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("userName", u.getUserName());
            m.put("customerCode", u.getCustomerCode());
            m.put("idNo", u.getIdNo());
            m.put("idType", u.getIdType());
            m.put("cuacctCls", u.getCuacctCls());
            m.put("cuacctStatus", u.getCuacctStatus());
            m.put("createdTime", u.getCreatedTime());
            list.add(m);
        }
        return Result.ok(list);
    }

    private void addPositionIfAny(long customerCode, String stkCode, Long balance) {
        if (!StringUtils.hasText(stkCode) || balance == null || balance <= 0) {
            return;
        }
        SecurityInfoRow sec = securityInfoMapper.findByStkCode(stkCode.trim());
        if (sec == null) {
            return;
        }
        long qty = balance;
        customerPositionMapper.insert(
                customerCode,
                sec.getMarket(),
                sec.getStkCode(),
                sec.getStkName(),
                qty,
                qty
        );
    }

    private long generateUniqueCustomerCode() {
        for (int i = 0; i < 30; i++) {
            long millis = System.currentTimeMillis();
            String ts7 = String.format("%07d", millis % 10_000_000L);
            int r4 = ThreadLocalRandom.current().nextInt(10_000);
            String r4s = String.format("%04d", r4);
            long code = Long.parseLong(ts7 + r4s);
            if (userInfoMapper.countByCustomerCode(code) == 0) {
                return code;
            }
        }
        throw new IllegalStateException("无法生成唯一客户代码，请重试");
    }

    private static String str(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        if (StringUtils.hasText(b)) {
            return b;
        }
        return null;
    }
}
