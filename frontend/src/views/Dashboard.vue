<template>
  <div class="container-fluid p-4">
    <h3 class="fw-bold mb-4">📊 Dashboard</h3>

    <!-- Summary Cards -->
    <div class="row g-3 mb-4">
      <div class="col-md-3" v-for="card in summaryCards" :key="card.title">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="fs-1">{{ card.icon }}</div>
            <div>
              <div class="text-muted small">{{ card.title }}</div>
              <div class="fw-bold fs-4">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Expenses Table -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-header bg-white fw-bold">Recent Expenses</div>
      <div class="card-body p-0">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th><th>Employee</th><th>Amount</th>
              <th>Category</th><th>Fraud Score</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="exp in recentExpenses" :key="exp.expId">
              <td>#{{ exp.expId }}</td>
              <td>Emp {{ exp.empId }}</td>
              <td>₹{{ Number(exp.amount).toLocaleString() }}</td>
              <td><span class="badge bg-secondary">{{ exp.category }}</span></td>
              <td>
                <span :class="fraudBadge(exp.fraudScore)">
                  {{ (exp.fraudScore * 100).toFixed(1) }}%
                </span>
              </td>
              <td><span :class="statusBadge(exp.status)">{{ exp.status }}</span></td>
            </tr>
            <tr v-if="recentExpenses.length === 0">
              <td colspan="6" class="text-center text-muted py-4">No expenses found</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Flagged Expenses Alert -->
    <div v-if="flaggedCount > 0" class="alert alert-danger d-flex align-items-center gap-2">
      ⚠️ <strong>{{ flaggedCount }} expense(s)</strong> have been flagged for potential fraud.
      <router-link to="/expenses" class="ms-2 alert-link">Review now →</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../services/api.js'

const expenses      = ref([])
const flaggedCount  = ref(0)

const recentExpenses = computed(() => expenses.value.slice(0, 8))

const summaryCards = computed(() => [
  { icon: '💰', title: 'Total Expenses',   value: expenses.value.length },
  { icon: '⏳', title: 'Pending Approval', value: expenses.value.filter(e => e.status === 'PENDING').length },
  { icon: '✅', title: 'Approved',         value: expenses.value.filter(e => e.status === 'APPROVED').length },
  { icon: '🚨', title: 'Fraud Flagged',    value: flaggedCount.value },
])

function fraudBadge(score) {
  const s = parseFloat(score)
  if (s > 0.7) return 'badge bg-danger'
  if (s > 0.4) return 'badge bg-warning text-dark'
  return 'badge bg-success'
}

function statusBadge(status) {
  const map = {
    PENDING:  'badge bg-warning text-dark',
    APPROVED: 'badge bg-success',
    REJECTED: 'badge bg-danger',
    FLAGGED:  'badge bg-danger',
  }
  return map[status] || 'badge bg-secondary'
}

onMounted(async () => {
  try {
    const [expRes, flagRes] = await Promise.all([
      api.getExpenses(),
      api.getFlaggedExpenses()
    ])
    expenses.value     = expRes.data
    flaggedCount.value = flagRes.data.length
  } catch (e) {
    console.error('Dashboard load error:', e)
  }
})
</script>
