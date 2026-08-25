import { useCallback, useEffect, useState } from 'react';
import { myNotifications } from '../api/notification';

const LAST_SEEN_KEY = 'vicinia_notifications_last_seen';
const POLL_MS = 30000;

/**
 * notification-service has no read/unread tracking server-side (Notification
 * has no `read` field) — "unseen" here is purely client-side: the newest
 * createdAt timestamp the user has actually opened the dropdown to see,
 * remembered in localStorage so it survives a refresh.
 */
export function useNotifications() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastSeen, setLastSeen] = useState(() => localStorage.getItem(LAST_SEEN_KEY) || '');

  const load = useCallback(() => {
    myNotifications()
      .then(setNotifications)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, POLL_MS);
    return () => clearInterval(interval);
  }, [load]);

  function markSeen() {
    if (notifications.length === 0) return;
    const newest = notifications[0].createdAt;
    localStorage.setItem(LAST_SEEN_KEY, newest);
    setLastSeen(newest);
  }

  const unseenCount = notifications.filter((n) => !lastSeen || n.createdAt > lastSeen).length;

  return { notifications, loading, unseenCount, markSeen, refresh: load };
}
