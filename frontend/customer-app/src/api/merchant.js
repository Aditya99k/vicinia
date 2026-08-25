import { apiClient } from './client';

export function apply(payload) {
  return apiClient.post('/api/merchants/apply', payload).then((r) => r.data);
}

export function getMe() {
  return apiClient.get('/api/merchants/me').then((r) => r.data);
}

export function updateMe(payload) {
  return apiClient.put('/api/merchants/me', payload).then((r) => r.data);
}

export function updateHours({ openTime, closeTime }) {
  return apiClient.put('/api/merchants/me/hours', { openTime, closeTime }).then((r) => r.data);
}

export function goLive() {
  return apiClient.post('/api/merchants/me/go-live').then((r) => r.data);
}

export function closeStore() {
  return apiClient.post('/api/merchants/me/close').then((r) => r.data);
}

export function reopenStore() {
  return apiClient.post('/api/merchants/me/reopen').then((r) => r.data);
}

export function nearby(city) {
  return apiClient.get('/api/merchants/nearby', { params: { city } }).then((r) => r.data);
}

// --- Merchant order queue ---------------------------------------------

export function pendingOrders() {
  return apiClient.get('/api/merchants/orders/pending').then((r) => r.data);
}

export function acceptOrder(orderId) {
  return apiClient.post(`/api/merchants/orders/${orderId}/accept`).then((r) => r.data);
}

export function rejectOrder(orderId, reason) {
  return apiClient.post(`/api/merchants/orders/${orderId}/reject`, { reason }).then((r) => r.data);
}

export function readyOrder(orderId) {
  return apiClient.post(`/api/merchants/orders/${orderId}/ready`).then((r) => r.data);
}

// --- Admin -----------------------------------------------------------

export function adminPending() {
  return apiClient.get('/api/merchants/admin/pending').then((r) => r.data);
}

export function adminApprove(id) {
  return apiClient.post(`/api/merchants/admin/${id}/approve`).then((r) => r.data);
}

export function adminReject(id, reason) {
  return apiClient.post(`/api/merchants/admin/${id}/reject`, { reason }).then((r) => r.data);
}

export function adminSuspend(id, reason) {
  return apiClient.post(`/api/merchants/admin/${id}/suspend`, { reason }).then((r) => r.data);
}

export function adminReinstate(id) {
  return apiClient.post(`/api/merchants/admin/${id}/reinstate`).then((r) => r.data);
}
