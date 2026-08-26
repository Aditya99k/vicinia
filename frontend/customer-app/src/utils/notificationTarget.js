const ORDER_ID_RE = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;

/**
 * NotificationResponse carries no structured target (no orderId/link field
 * — just eventType + free-text subject/body, both written by the backend
 * consumers that created them). Rather than add that field for one small
 * frontend feature, this derives a click destination from what's already
 * there: eventType picks the icon and general shape, the current user's
 * own role disambiguates the one eventType two different roles receive
 * ("order.confirmed" reads differently for the customer who placed it vs.
 * the merchant who now has to prepare it), and the order ID — always
 * present in these bodies' fixed phrasing, since this project writes them
 * itself — is pulled out with a UUID regex rather than string-matching
 * the exact sentence.
 */
export function notificationTarget(notification, role) {
  const orderId = notification.body?.match(ORDER_ID_RE)?.[0];

  switch (notification.eventType) {
    case 'order.confirmed':
      if (role === 'MERCHANT') return { icon: 'package', path: '/merchant#order-queue' };
      return { icon: 'check', path: orderId ? `/orders/${orderId}` : '/orders' };
    case 'payment.failed':
      return { icon: 'alert', path: orderId ? `/orders/${orderId}` : '/orders' };
    case 'inventory.low':
      return { icon: 'alert', path: '/merchant/listings' };
    case 'delivery.assigned':
      return { icon: 'truck', path: orderId ? `/delivery?orderId=${orderId}` : '/delivery' };
    case 'user.registered':
      return { icon: 'user', path: '/profile' };
    default:
      return { icon: 'bell', path: null };
  }
}
