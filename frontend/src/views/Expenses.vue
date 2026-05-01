<template>
  <div class="container-fluid p-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h3 class="fw-bold mb-0">💳 Expenses</h3>
      <button class="btn btn-dark" @click="showForm = !showForm">
        {{ showForm ? '✕ Cancel' : '+ Submit Expense' }}
      </button>
    </div>

    <!-- Submit Expense Form -->
    <div v-if="showForm" class="card border-0 shadow-sm mb-4">
      <div class="card-header bg-white fw-bold">Submit New Expense</div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label">Amount (₹)</label>
            <input v-model="form.amount" type="number" class="form-control"
                   placeholder="e.g. 2500" required />
          </div>
          <div class="col-md-4">
            <label class="form-label">Category</label>
            <select v-model="form.category" class="form-select">
              <option value="">Auto-detect by AI</option>
              <option value="TRAVEL">Travel</option>
              <option value="FOOD">Food</option>
              <option value="EQUIPMENT">Equipment</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">Description</label>
            <input v-model="form.description" type="text" class="form-control"
                   placeholder="e.g. Client visit to Mumbai" />
          </div>
        </div>
        <button class="btn btn-success mt-3" @click="submitExpense" :disabled="submitting">
          {{ submitting ? 'Submitting...' : '🚀 Submit & Analyze' }}
        </button>
        <div v-if="submitResult" class="mt-3 alert"
             :class="submitResult.status === 'FLAGGED' ? 'alert-danger' : 'alert-success'">
          <strong>Expense submitted!</strong>
          Category: {{ submitResult.category }} |
          Fraud Score: {{ (submitResult.fraudScore * 100).toFixed(1) }}% |
          Status: {{ submitResult.status }}
        </div>
      </div>
    </div>

    <!-- Filter Tabs -->
    <div class="d-flex gap-2 mb-3">
      <button v-for="tab in tabs" :key="tab"
              class="btn btn-sm"
              :class="activeTab === tab ? 'btn-dark' : 'btn-outline-secondary'"
              @click="activeTab = tab">
        {{ tab }}
      </button>
    </div>

    <!-- Expenses Table -->
    <div class="card border-0 shadow-sm">
      <div class="card-body p-0">
        <table class="table table-hover mb-0">
          <thead class="table-dark">
            <tr>
              <th>ID</th><th>Amount</th><th>Category</th>
              <th>Description</th><th>Fraud Score</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="exp in filteredExpenses" :key="exp.expId">
              <td class="fw-semibold">#{{ exp.expId }}</td>
              <td class="fw-bold">₹{{ Number(exp.amount).toLocaleString() }}</td>
              <td><span class="badge bg-secondary">{{ exp.category }}</span></td>
              <td class="text-truncate" style="max-width:200px">
                {{ exp.description || '—' }}
              </td>
              <td>
                <div class="progress" style="height:8px; width:80px">
                  <div class="progress-bar"
                       :class="exp.fraudScore > 0.7 ? 'bg-danger' : exp.fraudScore > 0.4 ? 'bg-warning' : 'bg-success'"
                       :style="`width: ${exp.fraudScore * 100}%`">
                  </div>
                </div>
                <small>{{ (exp.fraudScore * 100).toFixed(1) }}%</small>
              </td>
              <td><span :class="statusBadge(exp.status)">{{ exp.status }}</span></td>
              <td>
                <div v-if="exp.status === 'PENDING' || exp.status === 'FLAGGED'"
                     class="d-flex gap-1">
                  <button class="btn btn-xs btn-success btn-sm"
                          @click="updateStatus(exp.expId, 'APPROVED')">✓</button>
                  <button class="btn btn-xs btn-danger btn-sm"
                          @click="updateStatus(exp.expId, 'REJECTED')">✗</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../services/api.js'

const expenses    = ref([])
const showForm    = ref(false)
const submitting  = ref(false)
const submitResult = ref(null)
const activeTab   = ref('ALL')
const tabs        = ['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'FLAGGED']

const form = ref({ amount: '', category: '', description: '' })

const user = computed(() => {
  try { return JSON.parse(localStorage.getItem('user') || '{}') } catch { return {} }
})

const filteredExpenses = computed(() =>
  activeTab.value === 'ALL'
    ? expenses.value
    : expenses.value.filter(e => e.status === activeTab.value)
)

function statusBadge(status) {
  const map = { PENDING: 'badge bg-warning text-dark', APPROVED: 'badge bg-success',
                REJECTED: 'badge bg-danger', FLAGGED: 'badge bg-danger' }
  return map[status] || 'badge bg-secondary'
}

async function submitExpense() {
  submitting.value  = true
  submitResult.value = null
  try {
    const payload = {
      empId:       user.value.empId || 3,
      amount:      parseFloat(form.value.amount),
      description: form.value.description,
      category:    form.value.category || null,
    }
    const res = await api.submitExpense(payload)
    submitResult.value = res.data
    expenses.value.unshift(res.data)
    form.value = { amount: '', category: '', description: '' }
  } catch (e) {
    alert('Failed to submit expense: ' + (e.response?.data?.error || e.message))
  } finally {
    submitting.value = false
  }
}

async function updateStatus(expId, status) {
  try {
    await api.updateExpenseStatus(expId, { status })
    const idx = expenses.value.findIndex(e => e.expId === expId)
    if (idx !== -1) expenses.value[idx].status = status
  } catch (e) {
    alert('Failed to update: ' + e.message)
  }
}

onMounted(async () => {
  try {
    const res = await api.getExpenses()
    expenses.value = res.data
  } catch (e) { console.error(e) }
})
</script>
