/**
 * DeliveryHomePage now polls GET /api/delivery/tasks/mine as the real
 * source of truth for "do I have an active task" (Stage 18 bugfix — that
 * endpoint didn't used to exist). This local log remains purely for the
 * History screen, and as a same-device fallback the moment the page loads,
 * before the first poll has landed.
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
