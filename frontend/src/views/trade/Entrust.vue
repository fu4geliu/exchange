<template>
  <div class="page-card">
    <h2>委托页</h2>
    <div class="split-grid">
      <section class="section-card">
        <h3>行情参考</h3>
        <div class="quote-ref">
          <div class="tool-row">
            <label>股票代码：</label>
            <input v-model="quoteSymbol" @input="fillQuote" placeholder="输入代码，如 600446" />
          </div>
          <div class="quote-latest">最新价：{{ latestPrice }}</div>
        </div>

        <h3>提交委托</h3>
        <form @submit.prevent="onSubmitOrder">
          <div><label>客户号：</label><input v-model="form.customerCode" /></div>
          <div><label>股票代码：</label><input v-model="form.stkCode" /></div>
          <div>
            <label>买/卖：</label>
            <select v-model="form.trdId">
              <option value="B">买入</option>
              <option value="S">卖出</option>
            </select>
          </div>
          <div><label>委托数量：</label><input v-model.number="form.orderQty" type="number" /></div>
          <div><label>委托价格：</label><input v-model.number="form.orderPrice" type="number" step="0.01" /></div>
          <button type="submit">提交委托</button>
        </form>
      </section>

      <section class="section-card">
        <h3>当前委托列表</h3>
        <table>
          <thead>
            <tr><th>委托号</th><th>客户号</th><th>股票</th><th>买卖</th><th>数量</th><th>价格</th><th>状态</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in orders" :key="item.orderId">
              <td>{{ item.orderId }}</td>
              <td>{{ item.customerCode }}</td>
              <td>{{ item.stkCode }}</td>
              <td>{{ item.trdId === "B" ? "买入" : "卖出" }}</td>
              <td>{{ item.orderQty }}</td>
              <td>{{ item.orderPrice }}</td>
              <td>{{ item.orderStatus }}</td>
            </tr>
          </tbody>
        </table>
        <div class="pagination">
          <span>第 1 / 3 页</span>
          <button class="page-btn" type="button">上一页</button>
          <button class="page-btn active" type="button">1</button>
          <button class="page-btn" type="button">2</button>
          <button class="page-btn" type="button">3</button>
          <button class="page-btn" type="button">下一页</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useAppStore } from "../../store";
import { mockSubmitOrder, submitOrderApi } from "../../api/trade";

const appStore = useAppStore();
const quoteSymbol = ref("");
const latestPrice = ref("—");

const quoteMap = {
  "600446": 12.85,
  "000001": 9.81,
  "000858": 136.15
};

const form = ref({
  customerCode: "",
  stkCode: "",
  trdId: "B",
  orderQty: 0,
  orderPrice: 0
});

const orders = ref([]);

const fillQuote = () => {
  const p = quoteMap[quoteSymbol.value];
  if (p) {
    latestPrice.value = p;
    form.value.stkCode = quoteSymbol.value;
    form.value.orderPrice = p;
  }
};

const onSubmitOrder = async () => {
  const customerCode = String(form.value.customerCode || "").trim() || appStore.selectedCustomerCode;
  const payload = { ...form.value, customerCode };
  let data;
  try {
    const res = await submitOrderApi(payload);
    data = res.data;
  } catch {
    const fallback = await mockSubmitOrder(payload);
    data = fallback.data;
  }
  orders.value.unshift({
    orderId: data.data.orderId,
    customerCode,
    stkCode: form.value.stkCode,
    trdId: form.value.trdId,
    orderQty: form.value.orderQty,
    orderPrice: form.value.orderPrice,
    orderStatus: data.data.orderStatus
  });
};
</script>

<style scoped>
.quote-ref {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quote-latest {
  padding-left: 96px;
  color: #63543a;
  font-size: 14px;
}
</style>
