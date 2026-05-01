import { createRouter, createWebHistory } from 'vue-router'
import Login      from '../views/Login.vue'
import Dashboard  from '../views/Dashboard.vue'
import Expenses   from '../views/Expenses.vue'
import Procurement from '../views/Procurement.vue'
import Analytics  from '../views/Analytics.vue'

const routes = [
  { path: '/',          redirect: '/dashboard' },
  { path: '/login',     component: Login, meta: { public: true } },
  { path: '/dashboard', component: Dashboard,   meta: { requiresAuth: true } },
  { path: '/expenses',  component: Expenses,    meta: { requiresAuth: true } },
  { path: '/procurement', component: Procurement, meta: { requiresAuth: true } },
  { path: '/analytics', component: Analytics,   meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
