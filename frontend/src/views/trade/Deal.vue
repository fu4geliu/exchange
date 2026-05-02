<template>
  <div class="page-card">
    <h2>成交页</h2>
    <div class="split-grid">
      <section class="section-card">
        <h3>筛选条件</h3>
        <div class="tool-row">
          <label>客户号：</label>
          <input v-model="filters.customerCode" />
          <label>状态：</label>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="已报">已报</option>
            <option value="部分成交">部分成交</option>
          </select>
          <label>日期：</label>
          <input v-model="filters.date" type="date" />
          <button type="button" class="secondary" @click="query">查询</button>
        </div>
      </section>

      <section class="section-card">
        <h3>可成交委托列表</h3>
        <table>
          <thead>
            <tr><th>委托号</th><th>客户号</th><th>剩余量</th><th>状态</th><th>日期</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.orderId">
              <td>{{ item.orderId }}</td>
              <td>{{ item.customerCode }}</td>
              <td>{{ item.leftQty }}</td>
              <td>{{ item.status }}</td>
              <td>{{ item.date }}</td>
              <td><button type="button" @click="deal(item)">成交</button></td>
            </tr>
          </tbody>
        </table>
        <div class="pagination">
          <span>第 1 / 2 页</span>
          <button class="page-btn" type="button">上一页</button>
          <button class="page-btn active" type="button">1</button>
          <button class="page-btn" type="button">2</button>
          <button class="page-btn" type="button">下一页</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useAppStore } from "../../store";
import { dealOrderApi, mockSubmitDeal } from "../../api/trade";

const appStore = useAppStore();
const filters = ref({
  customerCode: "",
  status: "",
  date: ""
});

const sourceRows = ref([]);

const rows = computed(() =>
  sourceRows.value.filter((x) =>
    (!filters.value.customerCode || x.customerCode.includes(filters.value.customerCode)) &&
    (!filters.value.status || x.status === filters.value.status) &&
    (!filters.value.date || x.date === filters.value.date)
  )
);

const query = () => {
  // mock mode: computed rows update automatically by filters
};

const deal = async (item) => {
  try {
    await dealOrderApi(item.orderId, {
      customerCode: item.customerCode,
      tradeQty: item.leftQty,
      tradePrice: 12.66
    });
  } catch {
    await mockSubmitDeal({
      orderId: item.orderId,
      customerCode: item.customerCode,
      tradeQty: item.leftQty,
      tradePrice: 12.66
    });
  }
  sourceRows.value = sourceRows.value.filter((x) => x.orderId !== item.orderId);
};
</script>
