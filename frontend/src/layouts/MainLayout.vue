<script setup>
import { computed } from 'vue'
import { useRouter, RouterLink, RouterView } from 'vue-router'

const router = useRouter()
const currentUser = computed(() => localStorage.getItem('currentUser') || 'operator')

const menus = [
  { to: '/customer/open', label: '开户' },
  { to: '/trade/order', label: '委托下单' },
  { to: '/trade/cancel', label: '委托撤单' },
  { to: '/trade/execute', label: '模拟成交' },
  { to: '/trade/position', label: '持仓查询' },
  { to: '/trade/orders', label: '委托查询' },
  { to: '/trade/trades', label: '成交查询' },
]

const logout = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('currentUser')
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="title">金融交易系统</div>
      <nav class="menu">
        <RouterLink v-for="item in menus" :key="item.to" :to="item.to" class="menu-item">
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="user-panel">
        <span class="username">{{ currentUser }}</span>
        <button type="button" @click="logout">退出登录</button>
      </div>
    </header>

    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px 16px;
}

.title {
  font-weight: 700;
  color: #111827;
  min-width: 120px;
}

.menu {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.menu-item {
  padding: 6px 10px;
  border-radius: 6px;
  color: #334155;
  font-size: 14px;
}

.menu-item.router-link-active {
  background: #e8edff;
  color: #3247c7;
  font-weight: 600;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-size: 14px;
  color: #475569;
}

button {
  border: none;
  background: #ef4444;
  color: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
}

.page-content {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 20px 16px 40px;
}
</style>
