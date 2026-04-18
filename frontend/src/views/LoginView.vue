<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../api/http'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const backendBaseUrl = 'http://localhost:9801'

const handleLogin = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  if (!username.value.trim() || !password.value.trim()) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  loading.value = true

  try {
    const data = await request(`${backendBaseUrl}/api/auth/login`, {
      method: 'POST',
      skipExpiryCheck: true,
      body: JSON.stringify({
        username: username.value.trim(),
        password: password.value.trim(),
      }),
    })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('currentUser', data.username)

    successMessage.value = '登录成功'
    router.push('/customer/open')
  } catch {
    errorMessage.value = '登录失败，请检查账号密码（默认 operator / 123456）'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <h1>证券交易系统登录</h1>
      <p class="sub-title">请输入账号信息进入系统</p>

      <form @submit.prevent="handleLogin" class="login-form">
        <label for="username">用户名</label>
        <input
          id="username"
          v-model="username"
          type="text"
          placeholder="请输入用户名"
          autocomplete="username"
        />

        <label for="password">密码</label>
        <input
          id="password"
          v-model="password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
        />

        <button type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="message success">{{ successMessage }}</p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f5f7ff 0%, #eef1ff 100%);
  padding: 16px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 12px 32px rgba(33, 49, 108, 0.12);
}

h1 {
  margin: 0;
  font-size: 22px;
  color: #1f2a44;
}

.sub-title {
  margin: 8px 0 20px;
  color: #5f6880;
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

label {
  color: #2f3a57;
  font-size: 14px;
  font-weight: 600;
}

input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d4dcf0;
  border-radius: 8px;
  font-size: 14px;
}

input:focus {
  outline: none;
  border-color: #5568ff;
  box-shadow: 0 0 0 3px rgba(85, 104, 255, 0.14);
}

button {
  margin-top: 8px;
  height: 40px;
  border: 0;
  border-radius: 8px;
  background: #5568ff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.message {
  margin: 14px 0 0;
  font-size: 13px;
}

.error {
  color: #d92d20;
}

.success {
  color: #067647;
}
</style>
