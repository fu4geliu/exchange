<template>
  <div class="page-card">
    <h2>行情查询</h2>
    <div class="tool-row">
      <label>代码过滤：</label>
      <input v-model="keyword" placeholder="如 600" />
      <button class="secondary" type="button" @click="onLoad">加载行情（假数据）</button>
    </div>
    <table>
      <thead>
        <tr><th>代码</th><th>名称</th><th>当前价</th><th>涨跌幅</th><th>成交量</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredQuotes" :key="item.symbol">
          <td>{{ item.symbol }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.price }}</td>
          <td :class="item.changePct >= 0 ? 'tag-up' : 'tag-down'">{{ item.changePct }}%</td>
          <td>{{ item.volume }}</td>
        </tr>
      </tbody>
    </table>
    <div class="pagination">
      <span>第 1 / 5 页</span>
      <button class="page-btn" type="button">上一页</button>
      <button class="page-btn active" type="button">1</button>
      <button class="page-btn" type="button">2</button>
      <button class="page-btn" type="button">3</button>
      <button class="page-btn" type="button">4</button>
      <button class="page-btn" type="button">5</button>
      <button class="page-btn" type="button">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";

const keyword = ref("");
const quotes = ref([]);

const onLoad = async () => {
  quotes.value = [
    { symbol: "600446", name: "金证股份", price: 12.85, changePct: 4.38, volume: 320000 },
    { symbol: "000001", name: "平安银行", price: 9.81, changePct: 1.45, volume: 560000 },
    { symbol: "000858", name: "五粮液", price: 136.15, changePct: -1.91, volume: 90000 }
  ];
};

onLoad();

const filteredQuotes = computed(() => quotes.value.filter((x) => x.symbol.includes(keyword.value.trim())));
</script>
