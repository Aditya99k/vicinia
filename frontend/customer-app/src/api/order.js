import { apiClient } from './client';

export function placeOrder({ couponCode, paymentMethod, deliveryLatitude, deliveryLongitude }) {
  return apiClient.post('/api/orders', {
    couponCode: couponCode || null, paymentMethod,
    deliveryLatitude: deliveryLatitude ?? null, deliveryLongitude: deliveryLongitude ?? null,
  }).then((r) => r.data);
}

export function myOrders() {
  return apiClient.get('/api/orders/mine').then((r) => r.data);
}

export function getOrder(id) {
  return apiClient.get(`/api/orders/${id}`).then((r) => r.data);
}

export function cancelOrder(id, reason) {
  return apiClient.post(`/api/orders/${id}/cancel`, { reason }).then((r) => r.data);
}

export function merchantCancelOrder(id, reason) {
  return apiClient.post(`/api/orders/${id}/merchant-cancel`, { reason }).then((r) => r.data);
}

export function getOrderForMerchant(id) {
  return apiClient.get(`/api/orders/${id}/merchant-view`).then((r) => r.data);
}

export function getOrderForDelivery(id) {
  return apiClient.get(`/api/orders/${id}/delivery-view`).then((r) => r.data);
}
