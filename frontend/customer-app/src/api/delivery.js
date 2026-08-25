import { apiClient } from './client';

export function getMe() {
  return apiClient.get('/api/delivery/partners/me').then((r) => r.data);
}

export function goOnline({ latitude, longitude }) {
  return apiClient.post('/api/delivery/partners/online', { latitude, longitude }).then((r) => r.data);
}

export function goOffline() {
  return apiClient.post('/api/delivery/partners/offline').then((r) => r.data);
}

export function updateLocation({ latitude, longitude }) {
  return apiClient.post('/api/delivery/partners/location', { latitude, longitude }).then((r) => r.data);
}

export function acceptTask(orderId) {
  return apiClient.post(`/api/delivery/tasks/${orderId}/accept`).then((r) => r.data);
}

export function rejectTask(orderId) {
  return apiClient.post(`/api/delivery/tasks/${orderId}/reject`).then((r) => r.data);
}

export function pickedUp(orderId) {
  return apiClient.post(`/api/delivery/tasks/${orderId}/picked-up`).then((r) => r.data);
}

export function delivered(orderId) {
  return apiClient.post(`/api/delivery/tasks/${orderId}/delivered`).then((r) => r.data);
}
