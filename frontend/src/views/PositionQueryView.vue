<script setup>
import { reactive, ref } from 'vue'
import { request } from '../api/http'

const baseUrl = 'http://localhost:8082'
const form = reactive({ customerId: '', capitalAccountId: '' })
const rows = ref([])
const error = ref('')
const loading = ref(false)

const search = async () => {
  loading.value = true
  error.value = ''
  try {
    rows.value = await request(`${baseUrl}/api/trade/position/${Number(form.customerId)}/${Number(form.capitalAccountId)}`)
  } catch (e) {
    error.value = e.message || '查询失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page-card">
    <h2>持仓查询</h2>
    <p class="desc">根据客户ID和资金账户ID查询持仓。</p>
    <form class="form-grid" @submit.prevent="search">
      <input v-model="form.customerId" placeholder="客户ID" />
      <input v-model="form.capitalAccountId" placeholder="资金账户ID" />
      <button type="submit" :disabled="loading">{{ loading ? '查询中...' : '查询持仓' }}</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <pre class="table-placeholder">{{ rows }}</pre>
  </section>
</template>

<style scoped>
.page-card { background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:20px; }
h2 { margin:0 0 8px; }
.desc { margin:0 0 16px;color:#64748b; }
.form-grid { display:grid;gap:12px;margin-bottom:16px; }
input, button { height:40px;border-radius:8px;border:1px solid #d1d5db;padding:0 12px; }
button { background:#4f46e5;color:#fff;border:none;cursor:pointer; }
.table-placeholder { border:1px dashed #cbd5e1;border-radius:8px;padding:16px;color:#64748b; }
.error { margin-top:10px;color:#dc2626; }
</style>
