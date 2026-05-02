<template>
  <div class="layout">
    <aside class="sidebar">
      <div>
        <div class="brand">迷你证券交易控制台</div>
        <router-link to="/app/register" class="nav-item"><span class="nav-icon icon-register"></span>开户</router-link>
        <router-link to="/app/entrust" class="nav-item"><span class="nav-icon icon-entrust"></span>委托</router-link>
        <router-link to="/app/cancel" class="nav-item"><span class="nav-icon icon-cancel"></span>撤单</router-link>
        <router-link to="/app/deal" class="nav-item"><span class="nav-icon icon-deal"></span>成交</router-link>
        <router-link to="/app/market" class="nav-item"><span class="nav-icon icon-market"></span>行情</router-link>
      </div>

      <div class="sidebar-footer">
        <div>登录人：{{ appStore.operatorName || "操作员" }}</div>
        <button type="button" @click="logout">登出</button>
      </div>
    </aside>

    <main class="main">
      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useAppStore } from "../store";

const router = useRouter();
const appStore = useAppStore();

const logout = () => {
  appStore.logout();
  router.push("/login");
};
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
}

.sidebar {
  background: linear-gradient(180deg, #ecd9b4, #e2c894);
  border-right: 1px solid #cba86a;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  box-shadow: 6px 0 18px rgba(57, 42, 17, 0.08);
}

.brand {
  font-size: 18px;
  font-weight: 700;
  color: #4f3a16;
  margin-bottom: 14px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0;
  padding: 14px 14px;
  color: #4f3a16;
  text-decoration: none;
  border-radius: 10px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease,
    color 0.2s ease;
}

.nav-item:not(.router-link-active):hover {
  transform: translateX(4px);
  background: rgba(255, 252, 245, 0.55);
  box-shadow: 0 4px 12px rgba(120, 85, 35, 0.14);
  color: #3d2e12;
}

.nav-item:not(.router-link-active):hover .nav-icon {
  opacity: 1;
  border-color: #a07828;
  transform: scale(1.05);
}

.nav-item:not(.router-link-active):active {
  transform: translateX(2px);
  transition-duration: 0.08s;
}

.nav-item.router-link-active:hover {
  filter: brightness(1.05);
  box-shadow: 0 8px 16px rgba(168, 116, 35, 0.28);
}

.nav-icon {
  width: 18px;
  height: 18px;
  border: 1.6px solid #8b6a33;
  border-radius: 5px;
  position: relative;
  opacity: 0.88;
  flex-shrink: 0;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    opacity 0.2s ease;
}

.icon-register::after {
  content: "";
  position: absolute;
  left: 3px;
  right: 3px;
  top: 4px;
  height: 1.6px;
  background: #8b6a33;
  box-shadow: 0 4px 0 #8b6a33;
}

.icon-entrust::after {
  content: "";
  position: absolute;
  width: 8px;
  height: 8px;
  border: 1.6px solid #8b6a33;
  border-top: 0;
  border-left: 0;
  transform: rotate(45deg);
  left: 3px;
  top: 2px;
}

.icon-cancel::after {
  content: "";
  position: absolute;
  width: 8px;
  height: 8px;
  border: 1.6px solid #8b6a33;
  border-right: 0;
  border-bottom: 0;
  transform: rotate(-45deg);
  left: 3px;
  top: 3px;
}

.icon-deal::after {
  content: "";
  position: absolute;
  width: 7px;
  height: 4px;
  border-left: 1.8px solid #8b6a33;
  border-bottom: 1.8px solid #8b6a33;
  transform: rotate(-45deg);
  left: 4px;
  top: 4px;
}

.icon-market::after {
  content: "";
  position: absolute;
  left: 3px;
  bottom: 3px;
  width: 8px;
  height: 6px;
  border-left: 1.6px solid #8b6a33;
  border-bottom: 1.6px solid #8b6a33;
  transform: skewX(-25deg);
}

.nav-item.router-link-active {
  font-weight: 700;
  background: linear-gradient(135deg, #d9b066, #c8943f);
  color: #fff8ea;
  box-shadow: 0 6px 12px rgba(168, 116, 35, 0.22);
}

.sidebar-footer {
  border-top: 1px solid #caa56a;
  padding-top: 10px;
  display: grid;
  gap: 8px;
  color: #4f3a16;
}

.main {
  min-height: 100vh;
  background-image: linear-gradient(rgba(247, 242, 232, 0.35), rgba(247, 242, 232, 0.45)), url("../assets/images/beijing.png");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.content {
  padding: 16px;
  background: transparent;
  min-height: 100vh;
}
</style>
