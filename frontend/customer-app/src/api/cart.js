import { apiClient } from './client';

export function getCart() {
  return apiClient.get('/api/cart').then((r) => r.data);
}

export function addItem({ listingId, quantity }) {
  return apiClient.post('/api/cart/items', { listingId, quantity }).then((r) => r.data);
}

export function updateItem(listingId, quantity) {
  return apiClient.put(`/api/cart/items/${listingId}`, { quantity }).then((r) => r.data);
}

export function removeItem(listingId) {
  return apiClient.delete(`/api/cart/items/${listingId}`).then((r) => r.data);
}

export function clearCart() {
  return apiClient.delete('/api/cart').then((r) => r.data);
}
