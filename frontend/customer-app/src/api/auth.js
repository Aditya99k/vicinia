import { apiClient } from './client';

export function signup({ email, password, role }) {
  return apiClient.post('/api/auth/signup', { email, password, role }).then((r) => r.data);
}

export function login({ email, password }) {
  return apiClient.post('/api/auth/login', { email, password }).then((r) => r.data);
}

export function logout() {
  return apiClient.post('/api/auth/logout').then((r) => r.data);
}

export function forgotPassword(email) {
  return apiClient.post('/api/auth/forgot-password', { email }).then((r) => r.data);
}

export function resetPassword({ token, newPassword }) {
  return apiClient.post('/api/auth/reset-password', { token, newPassword }).then((r) => r.data);
}
