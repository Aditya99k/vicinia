import { apiClient } from './client';

export function getWalletBalance() {
  return apiClient.get('/api/payments/wallet/balance').then((r) => r.data);
}

export function topupWallet(amount) {
  return apiClient.post('/api/payments/wallet/topup', { amount }).then((r) => r.data);
}
