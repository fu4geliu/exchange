<script setup>
import { reactive, ref } from 'vue'
import { request } from '../api/http'

const baseUrl = 'http://localhost:8081'
const form = reactive({
  customerName: '',
  credentialTypeCode: '',
  credentialNumber: '',
  accountCategoryCode: '',
})
const result = ref(null)
const error = ref('')
const loading = ref(false)

const submit = async () => {
  loading.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await request(`${baseUrl}/api/customer/open`, {
      method: 'POST',
      body: JSON.stringify(form),
    })
  } catch (e) {
    error.value = e.message || '开户失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page-card">
    <h2>客户开户</h2>
    <p class="desc">录入客户名称、证件类别、证件号码、资产账户类别。</p>
    <form class="form-grid" @submit.prevent="submit">
      <input v-model="form.customerName" placeholder="客户名称（如：张三）" />
      <input v-model="form.credentialTypeCode" placeholder="证件类别代码（如：ID_CARD）" />
      <input v-model="form.credentialNumber" placeholder="证件号码" />
      <input v-model="form.accountCategoryCode" placeholder="账户类别代码（如：RETAIL）" />
      <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '提交开户' }}</button>
    </form>

    <p v-if="error" class="error">{{ error }}</p>
    <pre v-if="result" class="result">{{ result }}</pre>
  </section>
</template>

<style scoped>
.page-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

h2 {
  margin: 0 0 8px;
}

.desc {
  margin: 0 0 16px;
  color: #64748b;
}

.form-grid {
  display: grid;
  gap: 12px;
}

input,
button {
  height: 40px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 0 12px;
}

button {
  background: #4f46e5;
  color: #fff;
  border: none;
  cursor: pointer;
}

.error {
  margin-top: 10px;
  color: #dc2626;
}

.result {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
</style>
