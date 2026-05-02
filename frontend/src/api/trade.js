import request from "../utils/request";

// ===== 当前阶段：前端演示用假数据 =====
export const mockSubmitOrder = (payload) =>
  Promise.resolve({
    data: {
      success: true,
      message: "OK",
      data: {
        orderId: `O${Date.now()}`,
        orderStatus: "已报",
        ...payload
      }
    }
  });

export const mockSubmitWithdraw = (payload) =>
  Promise.resolve({
    data: {
      success: true,
      message: "OK",
      data: {
        withdrawId: `W${Date.now()}`,
        status: "已撤",
        ...payload
      }
    }
  });

export const mockSubmitDeal = (payload) =>
  Promise.resolve({
    data: {
      success: true,
      message: "OK",
      data: {
        dealId: `D${Date.now()}`,
        status: "已成",
        ...payload
      }
    }
  });

// ===== 留好的真实接口（后续直接切换调用）=====
export const submitOrderApi = (payload) => request.post("/api/trade/order", payload);
export const listOrdersApi = (params) => request.get("/api/trade/orders", { params });
export const listCancellableApi = (params) => request.get("/api/trade/cancellable", { params });
export const cancelOrderApi = (orderId, payload) => request.post(`/api/trade/orders/${orderId}/cancel`, payload);
export const listMatchableApi = (params) => request.get("/api/trade/matchable", { params });
export const dealOrderApi = (orderId, payload) => request.post(`/api/trade/orders/${orderId}/deals`, payload);
