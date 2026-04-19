<script setup>
import { reactive, ref } from 'vue'
import { request } from '../api/http'

const baseUrl = 'http://localhost:9802'
const form = reactive({
  customerCode: '',
  securityCode: '',
  price: '',
  quantity: '',
  directionCode: 'BUY',
})
const result = ref(null)
const error = ref('')
const loading = ref(false)

const submit = async () => {
  loading.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await request(`${baseUrl}/api/trade/order`, {
      method: 'POST',
      body: JSON.stringify({
        customerCode: String(form.customerCode).trim(),
        securityCode: form.securityCode,
        price: Number(form.price),
        quantity: Number(form.quantity),
        directionCode: form.directionCode,
      }),
    })
  } catch (e) {
    error.value = e.message || '委托下单失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page-card">
    <h2>委托下单</h2>
    <p class="desc">委托写入 order_info（客户代码对应 user_info.customer_code）。</p>
    <form class="form-grid" @submit.prevent="submit">
      <input v-model="form.customerCode" placeholder="客户代码（开户返回的 customerCode）" />
      <input v-model="form.securityCode" placeholder="证券代码（如 600446，须存在于 security_info）" />
      <input v-model="form.price" placeholder="委托价格" />
      <input v-model="form.quantity" placeholder="委托数量" />
      <select v-model="form.directionCode">
        <option value="BUY">买入</option>
        <option value="SELL">卖出</option>
      </select>
      <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '提交委托' }}</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <pre v-if="result" class="result">{{ result }}</pre>
  </section>
</template>

<style scoped>
.page-card { background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:20px; }
h2 { margin:0 0 8px; }
.desc { margin:0 0 16px;color:#64748b; }
.form-grid { display:grid;gap:12px; }
input, select, button { height:40px;border-radius:8px;border:1px solid #d1d5db;padding:0 12px; }
button { background:#4f46e5;color:#fff;border:none;cursor:pointer; }
.error { margin-top:10px;color:#dc2626; }
.result { margin-top:10px;padding:10px;border-radius:8px;background:#f8fafc;border:1px solid #e2e8f0; }
</style>
