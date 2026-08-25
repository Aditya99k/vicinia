import { apiClient } from './client';

export function validateCoupon(code, orderValue) {
  return apiClient.get('/api/coupons/validate', { params: { code, orderValue } }).then((r) => r.data);
}

export function applyCoupon({ code, orderId, orderValue }) {
  return apiClient.post('/api/coupons/apply', { code, orderId, orderValue }).then((r) => r.data);
}

// --- Admin -----------------------------------------------------------

export function adminCreateCoupon(payload) {
  return apiClient.post('/api/coupons/admin', payload).then((r) => r.data);
}

export function adminListCoupons() {
  return apiClient.get('/api/coupons/admin').then((r) => r.data);
}

export function adminUpdateCoupon(id, payload) {
  return apiClient.put(`/api/coupons/admin/${id}`, payload).then((r) => r.data);
}
