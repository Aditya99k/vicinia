import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { acceptTask, delivered, getMe, goOffline, goOnline, pickedUp, rejectTask, updateLocation } from '../../api/delivery';
import { getActiveTask, recordTask } from '../../utils/deliveryHistory';
import { NavigationIcon, PackageIcon, TruckIcon } from '../../components/Icons';
import { DeliveryIllustration } from '../../components/Illustrations';
import StatusBadge from '../../components/StatusBadge';

const NEXT_ACTION = {
  ASSIGNED: { label: 'Accept task', fn: acceptTask, next: 'ACCEPTED' },
  ACCEPTED: { label: 'Mark picked up', fn: pickedUp, next: 'PICKED_UP' },
  PICKED_UP: { label: 'Mark delivered', fn: delivered, next: 'DELIVERED' },
};

export default function DeliveryHomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [partner, setPartner] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);
  const [error, setError] = useState('');
  const [orderIdInput, setOrderIdInput] = useState('');
  const [activeTask, setActiveTask] = useState(getActiveTask());
  const [acting, setActing] = useState(false);

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

  async function handleToggleOnline() {
    setToggling(true);
    setError('');
    try {
      if (partner?.status === 'ONLINE') {
        await goOffline();
        load();
      } else if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          async (pos) => {
            await goOnline({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
            load();
            setToggling(false);
          },
          async () => {
            setError('Could not get your location — enable location access to go online.');
            setToggling(false);
          }
        );
        return;
      } else {
        setError('Your browser does not support location access.');
      }
    } catch {
      setError('Could not update your status.');
    } finally {
      if (!navigator.geolocation) setToggling(false);
    }
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
      setActiveTask(null);
    } catch {
      setError('Could not reject this task.');
    } finally {
      setActing(false);
    }
  }

  if (loading) return <div className="page-loading"><span className="spinner" /> Loading…</div>;

  const online = partner?.status === 'ONLINE';

  return (
    <div className="delivery-page">
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
        <div className="card active-task-card">
          <div className="active-task-head">
            <div className="stat-icon"><PackageIcon style={{ width: 18, height: 18 }} /></div>
            <div>
              <div style={{ fontWeight: 700 }}>Order #{activeTask.orderId.slice(0, 8)}</div>
              <StatusBadge status={activeTask.status} />
            </div>
          </div>
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
