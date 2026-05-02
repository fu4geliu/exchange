import axios from "axios";

const request = axios.create({
  baseURL: "http://localhost:9080",
  timeout: 15000
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status;
    const reqUrl = err.config?.url || "";

    // 登录接口本身的 401（账号密码错误）不重定向；其余 401（缺 Token、Token 过期等）回登录页
    if (status === 401 && !reqUrl.includes("/customer/login")) {
      localStorage.removeItem("token");
      const path = window.location.pathname || "";
      if (path !== "/login" && !path.startsWith("/login")) {
        const back = (path + (window.location.search || "")) || "/app/register";
        window.location.replace("/login?redirect=" + encodeURIComponent(back));
      }
    }

    const data = err.response?.data;
    const msg =
      (typeof data === "object" && data !== null && data.message) ||
      err.message ||
      "请求失败";
    const wrapped = new Error(msg);
    wrapped.status = status;
    wrapped.data = data;
    return Promise.reject(wrapped);
  }
);

export default request;
