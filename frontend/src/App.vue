<template>
  <div id="app">
    <!-- Navbar (hidden on login page) -->
    <nav v-if="isLoggedIn" class="navbar navbar-expand-lg navbar-dark bg-dark px-4">
      <a class="navbar-brand fw-bold" href="#">
        🏢 ProcureAI
      </a>
      <div class="navbar-nav ms-auto d-flex flex-row gap-3">
        <router-link class="nav-link" to="/dashboard">Dashboard</router-link>
        <router-link class="nav-link" to="/expenses">Expenses</router-link>
        <router-link class="nav-link" to="/procurement">Procurement</router-link>
        <router-link class="nav-link" to="/analytics">Analytics</router-link>
        <span class="nav-link text-warning">{{ user?.name }}</span>
        <a class="nav-link text-danger" href="#" @click.prevent="logout">Logout</a>
      </div>
    </nav>

    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const user = computed(() => {
  try { return JSON.parse(localStorage.getItem('user') || '{}') } catch { return {} }
})

function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>

<style>
body { background: #f4f6f9; }
.router-link-active { color: #ffc107 !important; font-weight: bold; }
</style>
