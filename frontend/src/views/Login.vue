<template>
  <div class="login-page">
    <div class="login-left-spacer"></div>
    <div class="login-right-panel">
      <div class="login-right">
        <h1>Exchange Console</h1>
        <p>专业、稳健、极简的迷你证券交易控制台</p>
      </div>
      <div class="login-card">
        <h2>操作员登录</h2>
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <form @submit.prevent="onSubmit">
          <div><label>账号：</label><input v-model="form.username" autocomplete="username" /></div>
          <div><label>密码：</label><input v-model="form.password" type="password" autocomplete="current-password" /></div>
          <button type="submit" :disabled="loading">{{ loading ? "登录中…" : "登录" }}</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { loginApi } from "../api/customer";
import { useAppStore } from "../store";

const router = useRouter();
const route = useRoute();
const appStore = useAppStore();
const form = ref({ username: "operator", password: "123456" });
const errorMsg = ref("");
const loading = ref(false);

const onSubmit = async () => {
  errorMsg.value = "";
  loading.value = true;
  try {
    const res = await loginApi({
      accountId: form.value.username,
      password: form.value.password
    });
    const body = res.data;
    if (!body.success || !body.data?.token) {
      errorMsg.value = body.message || "登录失败";
      return;
    }
    const token = body.data.token;
    localStorage.setItem("token", token);
    appStore.login(token, form.value.username);
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "";
    router.push(redirect && redirect.startsWith("/") ? redirect : "/app/register");
  } catch (e) {
    errorMsg.value = e.message || "登录失败";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  background-image: url("../assets/images/fenmian.png");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-page::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, rgba(251, 244, 232, 0.78), rgba(240, 226, 199, 0.62));
}

.login-left-spacer,
.login-right-panel {
  position: relative;
  z-index: 1;
}

.login-right-panel {
  padding: 118px 96px 40px 200px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.login-right {
  color: #4e3b21;
  text-align: left;
  margin-bottom: 100px;
}

.login-right h1 {
  margin: 0 0 10px;
  font-size: 40px;
}

.login-card {
  margin-top: 18px;
  width: min(430px, 92%);
  background: #fff;
  border: 1px solid #e7dbc5;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 12px 32px rgba(38, 30, 20, 0.12);
}

.error-msg {
  color: #a63d3d;
  font-size: 14px;
  margin: 0 0 12px;
}
</style>
