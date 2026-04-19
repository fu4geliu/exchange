<script setup>
import { reactive, ref } from 'vue'
import { request } from '../api/http'

const baseUrl = 'http://localhost:9801'
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
    <p class="desc">对应表 user_info / customer_account / customer_position。证件类型、账户类别使用字典项编码。</p>
    <form class="form-grid" @submit.prevent="submit">
      <input v-model="form.customerName" placeholder="客户姓名" />
      <input v-model="form.credentialTypeCode" placeholder="证件类型 ID_TYPE（00 身份证 / 01 护照 / 02 军官证）" />
      <input v-model="form.credentialNumber" placeholder="证件号码" />
      <input v-model="form.accountCategoryCode" placeholder="资产账户类别 CUACCT_CLS（0 散户 / 1 中户 / 2 大户 / 3 机构）" />
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
