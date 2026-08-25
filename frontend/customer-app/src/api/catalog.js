import { apiClient } from './client';

export function searchProducts({ q, category } = {}) {
  const params = {};
  if (q) params.q = q;
  if (category) params.category = category;
  return apiClient.get('/api/catalog/products/search', { params }).then((r) => r.data);
}

export function getProduct(id) {
  return apiClient.get(`/api/catalog/products/${id}`).then((r) => r.data);
}

export function getCategories() {
  return apiClient.get('/api/catalog/categories').then((r) => r.data);
}

export function requestProduct(payload) {
  return apiClient.post('/api/catalog/products/request', payload).then((r) => r.data);
}

export function myProducts() {
  return apiClient.get('/api/catalog/products/mine').then((r) => r.data);
}

// --- Admin -----------------------------------------------------------

export function adminCreateProduct(payload) {
  return apiClient.post('/api/catalog/admin/products', payload).then((r) => r.data);
}

export function adminPendingProducts() {
  return apiClient.get('/api/catalog/admin/products/pending').then((r) => r.data);
}

export function adminApproveProduct(id) {
  return apiClient.post(`/api/catalog/admin/products/${id}/approve`).then((r) => r.data);
}

export function adminRejectProduct(id, reason) {
  return apiClient.post(`/api/catalog/admin/products/${id}/reject`, { reason }).then((r) => r.data);
}

export function adminCreateCategory(name) {
  return apiClient.post('/api/catalog/admin/categories', { name }).then((r) => r.data);
}
