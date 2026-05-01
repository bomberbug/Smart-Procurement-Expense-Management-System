<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center bg-dark">
    <div class="card shadow-lg" style="width: 420px;">
      <div class="card-body p-5">
        <div class="text-center mb-4">
          <h2 class="fw-bold">🏢 ProcureAI</h2>
          <p class="text-muted">Smart Procurement System</p>
        </div>

        <div v-if="error" class="alert alert-danger">{{ error }}</div>

        <form @submit.prevent="handleLogin">
          <div class="mb-3">
            <label class="form-label fw-semibold">Email</label>
            <input v-model="form.email" type="email" class="form-control form-control-lg"
                   placeholder="admin@company.com" required />
          </div>
          <div class="mb-4">
            <label class="form-label fw-semibold">Password</label>
            <input v-model="form.password" type="password" class="form-control form-control-lg"
                   placeholder="••••••••" required />
          </div>
          <button type="submit" class="btn btn-dark btn-lg w-100" :disabled="loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="mt-4 p-3 bg-light rounded">
          <small class="text-muted d-block fw-bold mb-1">Demo Credentials:</small>
          <small class="text-muted d-block">admin@company.com / admin123</small>
          <small class="text-muted d-block">manager@company.com / manager123</small>
          <small class="text-muted d-block">employee@company.com / employee123</small>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api.js'

const router = useRouter()
const form    = ref({ email: '', password: '' })
const error   = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  error.value   = ''
  try {
    const res = await api.login(form.value)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data))
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.error || 'Login failed. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>
