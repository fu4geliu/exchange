import request from "../utils/request";

/** 登录（网关白名单，无需 Token）。Body：accountId + password */
export const loginApi = (payload) => request.post("/api/customer/login", payload);

/** 开户（须操作员 JWT，不在白名单） */
export const registerCustomerApi = (payload) =>
  request.post("/api/customer/register", payload);

/** 客户列表（须操作员 JWT） */
export const listCustomersApi = (params) =>
  request.get("/api/customer/list", { params: params || {} });

/** 开户页字典下拉（须操作员 JWT） */
export const registerDictsApi = () => request.get("/api/customer/register-dicts");
