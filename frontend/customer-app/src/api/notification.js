import { apiClient } from './client';

export function myNotifications() {
  return apiClient.get('/api/notifications/mine').then((r) => r.data);
}

export function clearMyNotifications() {
  return apiClient.delete('/api/notifications/mine');
}
