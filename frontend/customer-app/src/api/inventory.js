import { apiClient } from './client';

export function createListing({ productId, price, availableStock }) {
  return apiClient.post('/api/inventory/listings', { productId, price, availableStock }).then((r) => r.data);
}

export function myListings() {
  return apiClient.get('/api/inventory/listings/mine').then((r) => r.data);
}

export function updateListing(id, { price, availableStock, active }) {
  return apiClient.put(`/api/inventory/listings/${id}`, { price, availableStock, active }).then((r) => r.data);
}

export function listingsForProduct(productId) {
  return apiClient.get(`/api/inventory/listings/product/${productId}`).then((r) => r.data);
}

export function listingsForMerchant(merchantId) {
  return apiClient.get(`/api/inventory/listings/merchant/${merchantId}`).then((r) => r.data);
}

export function getListing(id) {
  return apiClient.get(`/api/inventory/listings/${id}`).then((r) => r.data);
}
