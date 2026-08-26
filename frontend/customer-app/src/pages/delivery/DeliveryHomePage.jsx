import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { acceptTask, delivered, getMe, goOffline, goOnline, myActiveTasks, pickedUp, rejectTask, updateLocation } from '../../api/delivery';
import { getOrderForDelivery } from '../../api/order';
import { useMerchantDirectory } from '../../hooks/useMerchantDirectory';
import { getActiveTask, recordTask } from '../../utils/deliveryHistory';
import { BanknoteIcon, CheckCircleIcon, MapPinIcon, NavigationIcon, PackageIcon, PhoneIcon, StoreIcon, TruckIcon } from '../../components/Icons';
import { DeliveryIllustration } from '../../components/Illustrations';
import StatusBadge from '../../components/StatusBadge';
import { formatMoney } from '../../utils/format';

const NEXT_ACTION = {
  ASSIGNED: { label: 'Accept task', fn: acceptTask, next: 'ACCEPTED', toast: 'Task accepted' },
  ACCEPTED: { label: 'Mark picked up', fn: pickedUp, next: 'PICKED_UP', toast: 'Marked picked up' },
  PICKED_UP: { label: 'Mark delivered', fn: delivered, next: 'DELIVERED', toast: 'Delivered' },
};

const POLL_MS = 6000;

export default function DeliveryHomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [partner, setPartner] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);
  const [error, setError] = useState('');
  const [orderIdInput, setOrderIdInput] = useState('');
  const [activeTask, setActiveTask] = useState(getActiveTask());
  const [orderInfo, setOrderInfo] = useState(null);
  const [acting, setActing] = useState(false);
  const [toast, setToast] = useState(null);
  const [flash, setFlash] = useState(false);
  const directory = useMerchantDirectory();

  function load() {
    getMe().then(setPartner).catch(() => setPartner(null)).finally(() => setLoading(false));
  }

  useEffect(load, []);

  // Arriving from a "New pickup assigned" notification link carries the
  // order straight through instead of making the rider retype it.
  useEffect(() => {
    const orderId = searchParams.get('orderId');
    if (!orderId) return;
    setActiveTask({ orderId, status: 'ASSIGNED', at: new Date().toISOString() });
    const next = new URLSearchParams(searchParams);
    next.delete('orderId');
    setSearchParams(next, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // The real source of truth for "do I have an active task" — polls
  // instead of depending solely on the notification deep link or a manual
  // Order ID entry, so a fresh assignment shows up here on its own.
  const syncActiveTask = useCallback(() => {
    myActiveTasks()
      .then((tasks) => {
        if (tasks.length > 0) {
          const t = tasks[0];
          setActiveTask((prev) =>
            prev?.orderId === t.orderId && prev?.status === t.status
              ? prev
              : { orderId: t.orderId, merchantId: t.merchantId, status: t.status, at: t.assignedAt || t.createdAt }
          );
        } else {
          setActiveTask((prev) => (prev ? null : prev));
        }
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    syncActiveTask();
    const interval = setInterval(syncActiveTask, POLL_MS);
    return () => clearInterval(interval);
  }, [syncActiveTask]);

  // What (if anything) needs collecting on handover — fetched once per task, not polled, since the payment method/paid state never changes mid-delivery.
  useEffect(() => {
    if (!activeTask?.orderId) {
      setOrderInfo(null);
      return;
    }
    getOrderForDelivery(activeTask.orderId).then(setOrderInfo).catch(() => setOrderInfo(null));
  }, [activeTask?.orderId]);

  function showToast(title, subtitle) {
    setToast({ title, subtitle });
    setTimeout(() => setToast(null), 3500);
  }

  function flashCard() {
    setFlash(true);
    setTimeout(() => setFlash(false), 900);
  }

  async function handleToggleOnline() {
    setToggling(true);
    setError('');

    // Going offline is a single synchronous call, so it's handled entirely
    // here. Going online waits on the browser's async geolocation prompt —
    // that path clears `toggling` itself, in whichever of its two
    // callbacks actually fires, never here.
    if (partner?.status === 'ONLINE') {
      try {
        await goOffline();
        load();
      } catch {
        setError('Could not update your status.');
      } finally {
        setToggling(false);
      }
      return;
    }

    if (!navigator.geolocation) {
      setError('Your browser does not support location access.');
      setToggling(false);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          await goOnline({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
          load();
        } catch {
          setError('Could not update your status.');
        } finally {
          setToggling(false);
        }
      },
      () => {
        setError('Could not get your location — enable location access to go online.');
        setToggling(false);
      }
    );
  }

  function pingLocation() {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition((pos) => {
      updateLocation({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }).catch(() => {});
    });
  }

  function loadTask(e) {
    e.preventDefault();
    const id = orderIdInput.trim();
    if (!id) return;
    setActiveTask({ orderId: id, status: 'ASSIGNED', at: new Date().toISOString() });
    setOrderIdInput('');
  }

  async function handleAction() {
    if (!activeTask) return;
    const action = NEXT_ACTION[activeTask.status];
    if (!action) return;
    setActing(true);
    setError('');
    try {
      await action.fn(activeTask.orderId);
      recordTask(activeTask.orderId, action.next);
      flashCard();
      showToast(action.toast, `Order #${activeTask.orderId.slice(0, 8)}`);
      setActiveTask(action.next === 'DELIVERED' ? null : { ...activeTask, status: action.next });
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not update this task.');
    } finally {
      setActing(false);
    }
  }

  async function handleRejectTask() {
    if (!activeTask) return;
    setActing(true);
    setError('');
    try {
      await rejectTask(activeTask.orderId);
      recordTask(activeTask.orderId, 'REJECTED');
      showToast('Task rejected', `Order #${activeTask.orderId.slice(0, 8)}`);
      setActiveTask(null);
    } catch {
      setError('Could not reject this task.');
    } finally {
      setActing(false);
    }
  }

  if (loading) return <div className="page-loading"><span className="spinner" /> Loading…</div>;

  const online = partner?.status === 'ONLINE';
  const storeName = activeTask?.merchantId ? directory.get(activeTask.merchantId)?.storeName : null;

  return (
    <div className="delivery-page">
      {toast && (
        <div className="new-order-toast">
          <span className="new-order-toast-icon"><CheckCircleIcon style={{ width: 16, height: 16 }} /></span>
          <div>
            <div style={{ fontWeight: 700 }}>{toast.title}</div>
            <div className="muted">{toast.subtitle}</div>
          </div>
        </div>
      )}

      <div className="delivery-status-card card">
        <div>
          <div className={`online-dot ${online ? 'on' : ''}`} />
          <h2 style={{ display: 'inline', marginLeft: 8 }}>{online ? "You're online" : "You're offline"}</h2>
          <p className="muted" style={{ marginTop: 4 }}>
            {online ? 'Nearby orders can be assigned to you.' : 'Go online to start receiving delivery tasks.'}
          </p>
        </div>
        <button className={`btn ${online ? 'btn-secondary' : 'btn-primary'}`} onClick={handleToggleOnline} disabled={toggling}>
          {toggling ? <span className="spinner" /> : online ? 'Go offline' : 'Go online'}
        </button>
      </div>

      {online && (
        <button className="btn-ghost" onClick={pingLocation} style={{ marginTop: 8 }}>
          <NavigationIcon style={{ width: 13, height: 13 }} /> Ping current location
        </button>
      )}

      {error && <div className="banner banner-error" style={{ marginTop: 16 }}>{error}</div>}

      <div className="section-title"><span>Active task</span></div>

      {activeTask ? (
        <div className={`card active-task-card ${flash ? 'action-flash' : ''}`}>
          <div className="active-task-head">
            <div className="stat-icon"><PackageIcon style={{ width: 18, height: 18 }} /></div>
            <div>
              <div style={{ fontWeight: 700 }}>Order #{activeTask.orderId.slice(0, 8)}</div>
              <StatusBadge status={activeTask.status} />
            </div>
          </div>

          {storeName && (
            <div className="active-task-store">
              <StoreIcon style={{ width: 14, height: 14 }} /> Pick up from <strong>{storeName}</strong>
            </div>
          )}

          {orderInfo?.dropoffAddress && (
            <div className="dropoff-address-block">
              <div className="pin"><MapPinIcon style={{ width: 15, height: 15 }} /></div>
              <div className="addr">
                <div className="label">Deliver to</div>
                <div className="value">{orderInfo.dropoffAddress}</div>
                {orderInfo.customerName && <div className="customer-name">{orderInfo.customerName}</div>}
              </div>
              {orderInfo.customerPhone && (
                <a className="btn btn-secondary btn-sm" href={`tel:${orderInfo.customerPhone}`}>
                  <PhoneIcon style={{ width: 13, height: 13 }} /> Call
                </a>
              )}
            </div>
          )}

          {orderInfo && (
            <div className={`collect-banner ${orderInfo.paid ? 'paid' : 'unpaid'}`}>
              {orderInfo.paid ? (
                <>
                  <CheckCircleIcon style={{ width: 16, height: 16 }} />
                  <span>Already paid — nothing to collect from the customer.</span>
                </>
              ) : (
                <>
                  <BanknoteIcon style={{ width: 16, height: 16 }} />
                  <span>Collect <strong>{formatMoney(orderInfo.totalAmount)}</strong> (cash or UPI) from the customer on delivery.</span>
                </>
              )}
            </div>
          )}

          {orderInfo?.dropoffLatitude != null && orderInfo?.dropoffLongitude != null && (
            <a
              className="navigate-dropoff-link"
              href={`https://www.google.com/maps/dir/?api=1&destination=${orderInfo.dropoffLatitude},${orderInfo.dropoffLongitude}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              <NavigationIcon style={{ width: 14, height: 14 }} /> Navigate to drop-off
            </a>
          )}

          <div className="merchant-order-actions" style={{ marginTop: 14 }}>
            {activeTask.status === 'ASSIGNED' && (
              <button className="btn btn-secondary btn-sm" onClick={handleRejectTask} disabled={acting}>Reject</button>
            )}
            {NEXT_ACTION[activeTask.status] && (
              <button className="btn btn-primary btn-sm" onClick={handleAction} disabled={acting}>
                {acting ? <span className="spinner" /> : NEXT_ACTION[activeTask.status].label}
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="card">
          <div className="empty-state" style={{ padding: '18px 8px' }}>
            <TruckIcon style={{ width: 64, height: 64, color: 'var(--faint)' }} />
            <h3>No active task</h3>
            <p>When dispatch assigns you an order, enter its Order ID below to start working it.</p>
          </div>
          <form onSubmit={loadTask} style={{ display: 'flex', gap: 8 }}>
            <input
              placeholder="Order ID"
              value={orderIdInput}
              onChange={(e) => setOrderIdInput(e.target.value)}
              style={{ flex: 1, border: '1.5px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '9px 12px', fontSize: 13.5, background: 'var(--bg)', color: 'var(--ink)' }}
            />
            <button className="btn btn-primary btn-sm">Load task</button>
          </form>
        </div>
      )}

      <DeliveryIllustration style={{ width: 200, margin: '32px auto 0', display: 'block', opacity: 0.85 }} />
    </div>
  );
}
