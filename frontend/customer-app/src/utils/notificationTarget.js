/**
 * NotificationResponse now carries referenceId (Stage 18 — the order or
 * product id this notification is about, set server-side by whichever
 * consumer created it) alongside the free-text subject/body. eventType
 * picks the icon and general shape; the current user's own role
 * disambiguates the one eventType two different roles receive
 * ("order.confirmed" reads differently for the customer who placed it vs.
 * the merchant who now has to prepare it).
 */
export function notificationTarget(notification, role) {
  const orderId = notification.referenceId;

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
