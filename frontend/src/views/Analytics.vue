<template>
  <div class="container-fluid p-4">
    <h3 class="fw-bold mb-4">📈 Analytics</h3>

    <div class="row g-4">
      <!-- Category Breakdown Doughnut -->
      <div class="col-md-5">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-header bg-white fw-bold">Expense by Category</div>
          <div class="card-body d-flex align-items-center justify-content-center">
            <canvas ref="doughnutChart" height="260"></canvas>
          </div>
        </div>
      </div>

      <!-- Fraud Stats Bar Chart -->
      <div class="col-md-7">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-header bg-white fw-bold">Fraud Score Distribution</div>
          <div class="card-body">
            <canvas ref="barChart" height="220"></canvas>
          </div>
        </div>
      </div>

      <!-- KPI Cards -->
      <div class="col-12">
        <div class="row g-3">
          <div class="col-md-3" v-for="kpi in kpiCards" :key="kpi.label">
            <div class="card border-0 shadow-sm text-center p-3">
              <div class="fs-2">{{ kpi.icon }}</div>
              <div class="fw-bold fs-4 mt-1">{{ kpi.value }}</div>
              <div class="text-muted small">{{ kpi.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Chart from 'chart.js/auto'
import api from '../services/api.js'

const doughnutChart = ref(null)
const barChart      = ref(null)
const kpiCards      = ref([])

onMounted(async () => {
  try {
    const [expRes, flagRes] = await Promise.all([
      api.getExpenses(),
      api.getFlaggedExpenses()
    ])

    const expenses = expRes.data
    const flagged  = flagRes.data

    // Category breakdown
    const catMap = {}
    expenses.forEach(e => {
      catMap[e.category] = (catMap[e.category] || 0) + 1
    })

    new Chart(doughnutChart.value, {
      type: 'doughnut',
      data: {
        labels: Object.keys(catMap),
        datasets: [{
          data: Object.values(catMap),
          backgroundColor: ['#343a40','#6c757d','#adb5bd','#ced4da'],
        }]
      },
      options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
    })

    // Fraud score buckets
    const buckets = { '0–20%': 0, '20–40%': 0, '40–60%': 0, '60–80%': 0, '80–100%': 0 }
    expenses.forEach(e => {
      const s = parseFloat(e.fraudScore) * 100
      if (s < 20)       buckets['0–20%']++
      else if (s < 40)  buckets['20–40%']++
      else if (s < 60)  buckets['40–60%']++
      else if (s < 80)  buckets['60–80%']++
      else              buckets['80–100%']++
    })

    new Chart(barChart.value, {
      type: 'bar',
      data: {
        labels: Object.keys(buckets),
        datasets: [{
          label: 'Number of Expenses',
          data: Object.values(buckets),
          backgroundColor: ['#198754','#20c997','#ffc107','#fd7e14','#dc3545'],
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
      }
    })

    // KPI cards
    const totalAmt = expenses.reduce((s, e) => s + parseFloat(e.amount || 0), 0)
    const approved = expenses.filter(e => e.status === 'APPROVED').length
    kpiCards.value = [
      { icon: '💰', label: 'Total Spend',      value: '₹' + totalAmt.toLocaleString() },
      { icon: '✅', label: 'Approved',          value: approved },
      { icon: '🚨', label: 'Fraud Flagged',     value: flagged.length },
      { icon: '📊', label: 'Avg Fraud Score',
        value: (expenses.reduce((s, e) => s + parseFloat(e.fraudScore || 0), 0)
                / (expenses.length || 1) * 100).toFixed(1) + '%' },
    ]

  } catch (e) { console.error(e) }
})
</script>
