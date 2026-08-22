import { apiClient } from './client';

export function getProfile() {
  return apiClient.get('/api/users/me/profile').then((r) => r.data);
}

export function updateProfile({ fullName, phone }) {
  return apiClient.put('/api/users/me/profile', { fullName, phone }).then((r) => r.data);
}

export function listAddresses() {
  return apiClient.get('/api/users/me/addresses').then((r) => r.data);
}

export function createAddress(payload) {
  return apiClient.post('/api/users/me/addresses', payload).then((r) => r.data);
}

export function updateAddress(id, payload) {
  return apiClient.put(`/api/users/me/addresses/${id}`, payload).then((r) => r.data);
}

export function deleteAddress(id) {
  return apiClient.delete(`/api/users/me/addresses/${id}`);
}
