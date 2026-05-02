const ID_15 = /^[1-9]\d{14}$/;
const ID_18 =
  /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
const PASSPORT = /^[a-zA-Z0-9]{5,20}$/;
const MILITARY = /^[\u4e00-\u9fa5a-zA-Z0-9\-]{6,20}$/;

/** GB11643 18位身份证校验位 */
function verifyId18Checksum(id18) {
  const s = id18.toUpperCase();
  if (s.length !== 18) return false;
  const w = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const check = "10X98765432";
  let sum = 0;
  for (let i = 0; i < 17; i++) {
    const d = s.charCodeAt(i) - 48;
    if (d < 0 || d > 9) return false;
    sum += d * w[i];
  }
  return check[sum % 11] === s[17];
}

export function validateUserName(name) {
  const t = (name || "").trim();
  if (!t) return "请输入姓名";
  if (t.length < 2 || t.length > 10) return "姓名须为 2～10 个字符";
  if (/[\r\n]/.test(t)) return "姓名不能含换行";
  return "";
}

export function validateIdNo(idType, idNoRaw) {
  const idNo = (idNoRaw || "").trim();
  if (!idNo) return "请输入证件号码";
  if (!idType) return "请选择证件类型";
  if (idType === "00") {
    if (idNo.length === 15) {
      return ID_15.test(idNo) ? "" : "15 位身份证号码须为 15 位数字且首位不为 0";
    }
    if (idNo.length === 18) {
      if (!ID_18.test(idNo)) return "18 位身份证号码格式不正确";
      return verifyId18Checksum(idNo) ? "" : "18 位身份证号码校验位不正确";
    }
    return "身份证号码长度应为 15 或 18 位";
  }
  if (idType === "01") {
    return PASSPORT.test(idNo) ? "" : "护照号应为 5～20 位字母或数字";
  }
  if (idType === "02") {
    return MILITARY.test(idNo) ? "" : "军官证号应为 6～20 位汉字、字母、数字或短横线";
  }
  return "";
}

/**
 * @param {string} pwdRaw 未填写时表示使用后端默认密码（前端不校验）
 */
export function validatePasswordOptional(pwdRaw) {
  const p = (pwdRaw || "").trim();
  if (!p) return "";
  if (p.length < 6 || p.length > 32) return "初始密码长度应为 6～32 位";
  if (/\s/.test(p)) return "密码不能包含空白字符";
  return "";
}

export function validateRegisterForm(form) {
  let msg = validateUserName(form.userName);
  if (msg) return msg;
  if (!form.idType) return "请选择证件类型";
  if (!form.cuacctCls) return "请选择账户类别";
  msg = validateIdNo(form.idType, form.idNo);
  if (msg) return msg;
  msg = validatePasswordOptional(form.password);
  if (msg) return msg;
  return "";
}
