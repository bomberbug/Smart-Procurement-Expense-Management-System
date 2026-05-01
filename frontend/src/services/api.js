import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// Attach JWT token to every request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Handle 401 globally
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default {
  // Auth
  login:    (data)         => api.post('/auth/login', data),

  // Expenses
  getExpenses:         ()       => api.get('/expenses'),
  getMyExpenses:       (empId)  => api.get(`/expenses/employee/${empId}`),
  submitExpense:       (data)   => api.post('/expenses', data),
  updateExpenseStatus: (id, data) => api.put(`/expenses/${id}/status`, data),
  getFlaggedExpenses:  ()       => api.get('/expenses/flagged'),
  getExpenseAnalytics: ()       => api.get('/expenses/analytics'),

  // Procurement
  getPurchaseOrders:   ()       => api.get('/procurement/orders'),
  createOrder:         (data)   => api.post('/procurement/orders', data),
  updateOrderStatus:   (id, data) => api.put(`/procurement/orders/${id}/status`, data),
  getVendors:          ()       => api.get('/procurement/vendors'),
  createVendor:        (data)   => api.post('/procurement/vendors', data),
}
