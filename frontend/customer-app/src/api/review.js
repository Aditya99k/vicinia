import { apiClient } from './client';

export function createReview({ productId, rating, comment }) {
  return apiClient.post('/api/reviews', { productId, rating, comment }).then((r) => r.data);
}

export function myReviews() {
  return apiClient.get('/api/reviews/mine').then((r) => r.data);
}

export function productReviews(productId) {
  return apiClient.get(`/api/reviews/products/${productId}`).then((r) => r.data);
}

export function productRating(productId) {
  return apiClient.get(`/api/reviews/products/${productId}/rating`).then((r) => r.data);
}
