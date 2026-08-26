import { useCallback, useEffect, useState } from 'react';
import { clearMyNotifications, myNotifications } from '../api/notification';
import { useAuth } from '../context/AuthContext';

const LAST_SEEN_KEY_PREFIX = 'vicinia_notifications_last_seen';
const POLL_MS = 30000;

/**
 * notification-service has no read/unread tracking server-side (Notification
 * has no `read` field) — "unseen" here is purely client-side: the newest
 * createdAt timestamp the user has actually opened the dropdown to see,
 * remembered in localStorage so it survives a refresh.
 *
 * Keyed by the current user's own id, not one flat key — a single shared
 * key meant a real bug on one browser used for multiple accounts (this
 * project's own normal test flow, switching between customer/merchant/
 * rider logins): opening the bell as one account overwrote the *other*
 * account's "last seen" marker, so logging back into the first account
 * showed everything as unseen again ("9+") even after it had already been
 * opened.
 */
export function useNotifications() {
  const { auth } = useAuth();
  const storageKey = auth?.userId ? `${LAST_SEEN_KEY_PREFIX}:${auth.userId}` : null;

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastSeen, setLastSeen] = useState(() => (storageKey ? localStorage.getItem(storageKey) || '' : ''));

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

  // Re-reads (rather than resets) on every account switch, so logging back
  // into an account that had already opened the bell doesn't show a false
  // "9+" from whatever the *other* account's key happens to hold.
  useEffect(() => {
    setLastSeen(storageKey ? localStorage.getItem(storageKey) || '' : '');
  }, [storageKey]);

  function markSeen() {
    if (!storageKey || notifications.length === 0) return;
    const newest = notifications[0].createdAt;
    localStorage.setItem(storageKey, newest);
    setLastSeen(newest);
  }

  function clearAll() {
    return clearMyNotifications().then(() => {
      setNotifications([]);
      if (storageKey) localStorage.removeItem(storageKey);
      setLastSeen('');
    });
  }

  const unseenCount = notifications.filter((n) => !lastSeen || n.createdAt > lastSeen).length;

  return { notifications, loading, unseenCount, lastSeen, markSeen, clearAll, refresh: load };
}
