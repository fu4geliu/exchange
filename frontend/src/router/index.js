import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/Login.vue";
import MainLayout from "../layouts/MainLayout.vue";
import Register from "../views/customer/Register.vue";
import Entrust from "../views/trade/Entrust.vue";
import Cancel from "../views/trade/Cancel.vue";
import Deal from "../views/trade/Deal.vue";
import Market from "../views/market/Market.vue";

const routes = [
  { path: "/", redirect: "/login" },
  { path: "/login", component: Login },
  {
    path: "/app",
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      { path: "", redirect: "/app/register" },
      { path: "register", component: Register },
      { path: "entrust", component: Entrust },
      { path: "cancel", component: Cancel },
      { path: "deal", component: Deal },
      { path: "market", component: Market }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.matched.some((r) => r.meta?.requiresAuth) && !localStorage.getItem("token")) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;
