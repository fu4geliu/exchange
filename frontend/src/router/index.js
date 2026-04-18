import { createRouter, createWebHistory } from 'vue-router'
import { isAccessTokenExpired } from '../api/jwt'
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import CustomerOpenView from '../views/CustomerOpenView.vue'
import TradeOrderView from '../views/TradeOrderView.vue'
import TradeCancelView from '../views/TradeCancelView.vue'
import TradeExecuteView from '../views/TradeExecuteView.vue'
import PositionQueryView from '../views/PositionQueryView.vue'
import OrderQueryView from '../views/OrderQueryView.vue'
import TradeQueryView from '../views/TradeQueryView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true },
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/customer/open' },
      { path: 'customer/open', name: 'customer-open', component: CustomerOpenView },
      { path: 'trade/order', name: 'trade-order', component: TradeOrderView },
      { path: 'trade/cancel', name: 'trade-cancel', component: TradeCancelView },
      { path: 'trade/execute', name: 'trade-execute', component: TradeExecuteView },
      { path: 'trade/position', name: 'trade-position', component: PositionQueryView },
      { path: 'trade/orders', name: 'trade-orders', component: OrderQueryView },
      { path: 'trade/trades', name: 'trade-trades', component: TradeQueryView },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  const isAuthenticated = Boolean(token) && !isAccessTokenExpired(token)

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
    return
  }

  if (to.meta.guestOnly && isAuthenticated) {
    next('/customer/open')
    return
  }

  next()
})

export default router
