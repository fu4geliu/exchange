package com.qianhua.customer.support;

import java.util.regex.Pattern;

/**
 * 开户表单：姓名、证件号、密码格式校验（与证件类型联动）。
 */
public final class RegisterPayloadValidator {

    private static final Pattern ID_18 = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
    private static final Pattern ID_15 = Pattern.compile("^[1-9]\\d{14}$");
    private static final Pattern PASSPORT = Pattern.compile("^[a-zA-Z0-9]{5,20}$");
    private static final Pattern MILITARY = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9\\-]{6,20}$");

    private RegisterPayloadValidator() {
    }

    public static String validateUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            return "请输入姓名";
        }
        String t = userName.trim();
        if (t.length() < 2 || t.length() > 10) {
            return "姓名须为 2～10 个字符";
        }
        if (t.indexOf('\n') >= 0 || t.indexOf('\r') >= 0) {
            return "姓名不能含换行";
        }
        return null;
    }

    /**
     * @param idType 字典 ID_TYPE 的 item_code：00/01/02
     */
    public static String validateIdNo(String idType, String idNo) {
        if (idNo == null || idNo.isBlank()) {
            return "请输入证件号码";
        }
        String s = idNo.trim();
        return switch (idType) {
            case "00" -> validateChineseId(s);
            case "01" -> validatePassport(s);
            case "02" -> validateMilitary(s);
            default -> "不支持的证件类型";
        };
    }

    private static String validateChineseId(String s) {
        if (s.length() == 15) {
            if (!ID_15.matcher(s).matches()) {
                return "15 位身份证号码须为 15 位数字且首位不为 0";
            }
            return null;
        }
        if (s.length() == 18) {
            if (!ID_18.matcher(s).matches()) {
                return "18 位身份证号码格式不正确（出生日期或末位）";
            }
            if (!verifyChineseId18Checksum(s)) {
                return "18 位身份证号码校验位不正确";
            }
            return null;
        }
        return "身份证号码长度应为 15 或 18 位";
    }

    /** 公民身份号码 GB11643 校验码 */
    private static boolean verifyChineseId18Checksum(String id18) {
        if (id18 == null || id18.length() != 18) {
            return false;
        }
        char[] chars = id18.toUpperCase().toCharArray();
        int[] w = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        String check = "10X98765432";
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int d = chars[i] - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            sum += d * w[i];
        }
        char expected = check.charAt(sum % 11);
        char actual = Character.toUpperCase(chars[17]);
        return actual == expected;
    }

    private static String validatePassport(String s) {
        if (!PASSPORT.matcher(s).matches()) {
            return "护照号应为 5～20 位字母或数字";
        }
        return null;
    }

    private static String validateMilitary(String s) {
        if (!MILITARY.matcher(s).matches()) {
            return "军官证号应为 6～20 位汉字、字母、数字或短横线";
        }
        return null;
    }

    public static String validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return "密码不能为空";
        }
        String p = rawPassword.trim();
        if (p.length() < 6 || p.length() > 32) {
            return "初始密码长度应为 6～32 位";
        }
        if (p.contains(" ") || p.contains("\t")) {
            return "密码不能包含空白字符";
        }
        return null;
    }
}
