import { useEffect, useRef, useState } from 'react';
import { useNotifications } from '../hooks/useNotifications';
import { BellIcon } from './Icons';
import { formatDateTime } from '../utils/format';

export default function NotificationsBell() {
  const { notifications, loading, unseenCount, markSeen } = useNotifications();
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);

  useEffect(() => {
    function onOutsideClick(e) {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener('mousedown', onOutsideClick);
    return () => document.removeEventListener('mousedown', onOutsideClick);
  }, []);

  function toggle() {
    setOpen((o) => {
      if (!o) markSeen();
      return !o;
    });
  }

  return (
    <div className="notifications-wrap" ref={wrapRef}>
      <button className="icon-btn cart-btn" onClick={toggle} aria-label="Notifications">
        <BellIcon style={{ width: 18, height: 18 }} />
        {unseenCount > 0 && <span className="cart-count">{unseenCount > 9 ? '9+' : unseenCount}</span>}
      </button>

      {open && (
        <div className="notifications-dropdown">
          <div className="notifications-header">Notifications</div>
          {loading ? (
            <div className="search-suggestion-loading"><span className="spinner" /></div>
          ) : notifications.length === 0 ? (
            <div className="search-suggestion-empty">Nothing yet — you'll see updates here as they happen.</div>
          ) : (
            <div className="notifications-list">
              {notifications.slice(0, 20).map((n) => (
                <div className="notification-row" key={n.id}>
                  <div className="subject">{n.subject}</div>
                  <div className="body">{n.body}</div>
                  <div className="muted">{formatDateTime(n.createdAt)}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
