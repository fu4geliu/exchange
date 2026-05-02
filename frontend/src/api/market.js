import request from "../utils/request";

// ===== 当前阶段：前端演示用假数据 =====
export const mockFetchQuotes = () =>
  Promise.resolve({
    data: {
      success: true,
      message: "OK",
      data: [
        { symbol: "600446", name: "金证股份", price: 12.85, changePct: 4.38 },
        { symbol: "000001", name: "平安银行", price: 9.81, changePct: 1.45 },
        { symbol: "000858", name: "五粮液", price: 136.15, changePct: 1.91 }
      ]
    }
  });

// ===== 留好的真实接口（后续直接切换调用）=====
export const quoteApi = (symbol) => request.get("/api/market/quote", { params: { symbol } });
export const quotesApi = (symbols) => request.get("/api/market/quotes", { params: { symbols } });
export const detailApi = (symbol) => request.get("/api/market/detail", { params: { symbol } });
