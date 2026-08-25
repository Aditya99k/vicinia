import { apiClient } from './client';

export function mySettlements() {
  return apiClient.get('/api/settlements/mine').then((r) => r.data);
}

export function myPayouts() {
  return apiClient.get('/api/settlements/payouts/mine').then((r) => r.data);
}

// --- Admin -----------------------------------------------------------

export function adminEntries() {
  return apiClient.get('/api/settlements/admin/entries').then((r) => r.data);
}

export function adminPayouts() {
  return apiClient.get('/api/settlements/admin/payouts').then((r) => r.data);
}

export function adminRunBatch() {
  return apiClient.post('/api/settlements/admin/payouts/run-batch').then((r) => r.data);
}

export function adminRunProcessor() {
  return apiClient.post('/api/settlements/admin/payouts/run-processor').then((r) => r.data);
}
