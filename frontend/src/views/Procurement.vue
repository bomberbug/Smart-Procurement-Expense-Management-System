<template>
  <div class="container-fluid p-4">
    <h3 class="fw-bold mb-4">📦 Procurement</h3>

    <div class="row g-4">
      <!-- Purchase Orders -->
      <div class="col-lg-8">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <span class="fw-bold">Purchase Orders</span>
            <button class="btn btn-sm btn-dark" @click="showOrderForm = !showOrderForm">
              + New Order
            </button>
          </div>

          <!-- New Order Form -->
          <div v-if="showOrderForm" class="card-body border-bottom">
            <div class="row g-2">
              <div class="col-md-4">
                <input v-model="orderForm.description" class="form-control"
                       placeholder="Description" />
              </div>
              <div class="col-md-3">
                <input v-model="orderForm.amount" type="number" class="form-control"
                       placeholder="Amount (₹)" />
              </div>
              <div class="col-md-3">
                <select v-model="orderForm.vendorId" class="form-select">
                  <option value="">Select Vendor</option>
                  <option v-for="v in vendors" :key="v.vendorId" :value="v.vendorId">
                    {{ v.name }}
                  </option>
                </select>
              </div>
              <div class="col-md-2">
                <button class="btn btn-success w-100" @click="createOrder">Create</button>
              </div>
            </div>
          </div>

          <div class="card-body p-0">
            <table class="table table-hover mb-0">
              <thead class="table-light">
                <tr>
                  <th>PO #</th><th>Description</th><th>Amount</th>
                  <th>Vendor</th><th>Status</th><th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="po in orders" :key="po.poId">
                  <td class="fw-semibold">PO-{{ po.poId }}</td>
                  <td>{{ po.description }}</td>
                  <td>₹{{ Number(po.amount).toLocaleString() }}</td>
                  <td>{{ getVendorName(po.vendorId) }}</td>
                  <td><span :class="statusBadge(po.status)">{{ po.status }}</span></td>
                  <td>
                    <div v-if="po.status === 'PENDING'" class="d-flex gap-1">
                      <button class="btn btn-sm btn-success"
                              @click="approveOrder(po.poId, 'APPROVED')">Approve</button>
                      <button class="btn btn-sm btn-danger"
                              @click="approveOrder(po.poId, 'REJECTED')">Reject</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Vendor Panel -->
      <div class="col-lg-4">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white fw-bold">🏭 Vendors</div>
          <div class="card-body p-0">
            <div v-for="v in vendors" :key="v.vendorId"
                 class="d-flex align-items-center justify-content-between p-3 border-bottom">
              <div>
                <div class="fw-semibold">{{ v.name }}</div>
                <small class="text-muted">{{ v.email }}</small>
              </div>
              <div class="text-end">
                <div class="fw-bold text-warning">★ {{ v.rating }}</div>
                <small class="text-muted">{{ v.contact }}</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../services/api.js'

const orders        = ref([])
const vendors       = ref([])
const showOrderForm = ref(false)
const orderForm     = ref({ description: '', amount: '', vendorId: '', deptId: 1 })

const user = () => {
  try { return JSON.parse(localStorage.getItem('user') || '{}') } catch { return {} }
}

function getVendorName(vendorId) {
  return vendors.value.find(v => v.vendorId === vendorId)?.name || `Vendor ${vendorId}`
}

function statusBadge(status) {
  const map = { DRAFT: 'badge bg-secondary', PENDING: 'badge bg-warning text-dark',
                APPROVED: 'badge bg-success', REJECTED: 'badge bg-danger',
                COMPLETED: 'badge bg-primary' }
  return map[status] || 'badge bg-secondary'
}

async function createOrder() {
  try {
    const payload = {
      description: orderForm.value.description,
      amount:      parseFloat(orderForm.value.amount),
      vendorId:    parseInt(orderForm.value.vendorId),
      deptId:      1,
      requestedBy: user().empId || 3,
      status:      'PENDING'
    }
    const res = await api.createOrder(payload)
    orders.value.unshift(res.data)
    orderForm.value = { description: '', amount: '', vendorId: '', deptId: 1 }
    showOrderForm.value = false
  } catch (e) {
    alert('Failed: ' + (e.response?.data?.error || e.message))
  }
}

async function approveOrder(poId, status) {
  try {
    await api.updateOrderStatus(poId, { status })
    const idx = orders.value.findIndex(o => o.poId === poId)
    if (idx !== -1) orders.value[idx].status = status
  } catch (e) { alert('Failed: ' + e.message) }
}

onMounted(async () => {
  try {
    const [ordRes, venRes] = await Promise.all([
      api.getPurchaseOrders(),
      api.getVendors()
    ])
    orders.value  = ordRes.data
    vendors.value = venRes.data
  } catch (e) { console.error(e) }
})
</script>
