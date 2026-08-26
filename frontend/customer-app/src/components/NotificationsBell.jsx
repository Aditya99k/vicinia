import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { useAuth } from '../context/AuthContext';
import { primaryRole } from '../utils/roles';
import { notificationTarget } from '../utils/notificationTarget';
import { AlertIcon, BellIcon, CheckCircleIcon, PackageIcon, TruckIcon, UserIcon } from './Icons';
import { formatDateTime } from '../utils/format';

const ICONS = {
  check: CheckCircleIcon,
  package: PackageIcon,
  alert: AlertIcon,
  truck: TruckIcon,
  user: UserIcon,
  bell: BellIcon,
};

const TONES = {
  check: 'success',
  package: 'brand',
  alert: 'danger',
  truck: 'brand',
  user: 'muted',
  bell: 'muted',
};

export default function NotificationsBell() {
  const { notifications, loading, unseenCount, markSeen, clearAll, lastSeen } = useNotifications();
  const { auth } = useAuth();
  const role = primaryRole(auth);
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [clearing, setClearing] = useState(false);
  const wrapRef = useRef(null);

  async function handleClearAll(e) {
    e.stopPropagation();
    setClearing(true);
    try {
      await clearAll();
    } finally {
      setClearing(false);
    }
  }

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

  function handleClick(n) {
    const { path } = notificationTarget(n, role);
    setOpen(false);
    if (path) navigate(path);
  }

  return (
    <div className="notifications-wrap" ref={wrapRef}>
      <button className="icon-btn cart-btn" onClick={toggle} aria-label="Notifications">
        <BellIcon style={{ width: 18, height: 18 }} />
        {unseenCount > 0 && <span className="cart-count">{unseenCount > 9 ? '9+' : unseenCount}</span>}
      </button>

      {open && (
        <div className="notifications-dropdown">
          <div className="notifications-header">
            <span>Notifications</span>
            {notifications.length > 0 && (
              <button type="button" className="notifications-clear-btn" onClick={handleClearAll} disabled={clearing}>
                {clearing ? <span className="spinner" /> : 'Clear all'}
              </button>
            )}
          </div>
          {loading ? (
            <div className="search-suggestion-loading"><span className="spinner" /></div>
          ) : notifications.length === 0 ? (
            <div className="search-suggestion-empty">Nothing yet — you'll see updates here as they happen.</div>
          ) : (
            <div className="notifications-list">
              {notifications.slice(0, 20).map((n) => {
                const { icon, path } = notificationTarget(n, role);
                const Icon = ICONS[icon];
                const tone = TONES[icon];
                const unseen = !lastSeen || n.createdAt > lastSeen;
                return (
                  <button
                    type="button"
                    className={`notification-row ${path ? 'clickable' : ''} ${unseen ? 'unseen' : ''}`}
                    key={n.id}
                    onClick={() => handleClick(n)}
                    disabled={!path}
                  >
                    <div className={`notification-icon tone-${tone}`}><Icon style={{ width: 15, height: 15 }} /></div>
                    <div className="notification-body-col">
                      <div className="subject">{n.subject}</div>
                      <div className="body">{n.body}</div>
                      <div className="muted">{formatDateTime(n.createdAt)}</div>
                    </div>
                    {unseen && <span className="notification-dot" />}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
