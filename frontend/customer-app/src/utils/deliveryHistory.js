/**
 * delivery-service exposes no "list my assigned tasks" endpoint (a task is
 * only reachable by orderId, via accept/reject/picked-up/delivered) and
 * notification-service has no delivery-assignment consumer — so there is
 * no server-side signal a partner can poll to discover a new task. This
 * keeps a small local log of orders this device has acted on, purely for
 * the History screen; it's not a substitute for a real task inbox.
 */
const KEY = 'vicinia_delivery_history';

export function getHistory() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || [];
  } catch {
    return [];
  }
}

export function recordTask(orderId, status) {
  const history = getHistory().filter((h) => h.orderId !== orderId);
  history.unshift({ orderId, status, at: new Date().toISOString() });
  localStorage.setItem(KEY, JSON.stringify(history.slice(0, 30)));
}

export function getActiveTask() {
  return getHistory().find((h) => h.status !== 'DELIVERED') || null;
}
