import { titleCase } from './format';

/** Maps a domain status string to a badge tone — shared across order/merchant/delivery/settlement status pills so "green means good, red means bad, yellow means waiting" stays consistent everywhere in the app. */
const TONES = {
  // OrderStatus
  CREATED: 'muted',
  PAYMENT_PENDING: 'warn',
  CONFIRMED: 'brand',
  MERCHANT_ACCEPTED: 'brand',
  PREPARING: 'brand',
  READY_FOR_PICKUP: 'warn',
  DELIVERY_ASSIGNED: 'warn',
  OUT_FOR_DELIVERY: 'warn',
  DELIVERED: 'success',
  PAYMENT_FAILED: 'danger',
  CANCELLED: 'danger',
  MERCHANT_REJECTED: 'danger',
  REFUND_PENDING: 'warn',
  REFUNDED: 'muted',

  // MerchantStatus
  PENDING_REVIEW: 'warn',
  APPROVED: 'success',
  REJECTED: 'danger',
  ONBOARDING: 'warn',
  LIVE: 'success',
  SUSPENDED: 'danger',
  TEMP_CLOSED: 'muted',
  PERMANENTLY_CLOSED: 'danger',

  // OrderTaskStatus
  PENDING_ACCEPTANCE: 'warn',
  ACCEPTED: 'brand',
  READY: 'success',

  // DeliveryTaskStatus / PartnerStatus
  PENDING_ASSIGNMENT: 'warn',
  ASSIGNED: 'warn',
  PICKED_UP: 'brand',
  ONLINE: 'success',
  OFFLINE: 'muted',

  // SettlementEntryStatus / PayoutStatus
  PENDING: 'warn',
  PROCESSING: 'warn',
  SETTLED: 'success',
  PAID: 'success',
  FAILED: 'danger',
};

export function statusTone(status) {
  return TONES[status] || 'muted';
}

export function statusLabel(status) {
  return titleCase(status);
}

export const TONE_CLASS = {
  success: 'badge-success',
  danger: 'badge-danger',
  warn: 'badge-warn',
  brand: 'badge',
  muted: 'badge-muted',
};
