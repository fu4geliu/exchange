<script setup>
import { reactive, ref } from 'vue'
import { request } from '../api/http'

const baseUrl = 'http://localhost:8082'
const form = reactive({ orderId: '', tradePrice: '', tradeQuantity: '' })
const result = ref(null)
const error = ref('')
const loading = ref(false)

const submit = async () => {
  loading.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await request(`${baseUrl}/api/trade/execute`, {
      method: 'POST',
      body: JSON.stringify({
        orderId: Number(form.orderId),
        tradePrice: Number(form.tradePrice),
        tradeQuantity: Number(form.tradeQuantity),
      }),
    })
  } catch (e) {
    error.value = e.message || '成交失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page-card">
    <h2>模拟成交</h2>
    <p class="desc">录入成交价格和数量，模拟撮合成交。</p>
    <form class="form-grid" @submit.prevent="submit">
      <input v-model="form.orderId" placeholder="委托ID" />
      <input v-model="form.tradePrice" placeholder="成交价格" />
      <input v-model="form.tradeQuantity" placeholder="成交数量" />
      <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '提交成交' }}</button>
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
input, button { height:40px;border-radius:8px;border:1px solid #d1d5db;padding:0 12px; }
button { background:#4f46e5;color:#fff;border:none;cursor:pointer; }
.error { margin-top:10px;color:#dc2626; }
.result { margin-top:10px;padding:10px;border-radius:8px;background:#f8fafc;border:1px solid #e2e8f0; }
</style>
