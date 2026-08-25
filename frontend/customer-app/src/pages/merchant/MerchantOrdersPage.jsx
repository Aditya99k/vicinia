import { useEffect, useState } from 'react';
import { acceptOrder, pendingOrders, readyOrder, rejectOrder } from '../../api/merchant';
import StatusBadge from '../../components/StatusBadge';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { formatDateTime } from '../../utils/format';
import { useActionDialog } from '../../hooks/useActionDialog';

export default function MerchantOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');
  const { promptText, dialog } = useActionDialog();

  function load() {
    setLoading(true);
    pendingOrders().then(setOrders).catch(() => setError('Could not load your order queue.')).finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function act(orderId, fn) {
    setBusyId(orderId);
    setError('');
    try {
      await fn(orderId);
      load();
    } catch {
      setError('Could not update this order.');
    } finally {
      setBusyId(null);
    }
  }

  async function handleReject(orderId) {
    const reason = await promptText('Reason for rejecting this order?', { title: 'Reject order', placeholder: 'e.g. Out of stock on one of the items' });
    if (reason === null) return;
    setBusyId(orderId);
    setError('');
    try {
      await rejectOrder(orderId, reason || 'Unable to fulfil');
      load();
    } catch {
      setError('Could not reject this order.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      {dialog}
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Order queue</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Accept incoming orders and mark them ready for pickup.</p>

      {error && <div className="banner banner-error">{error}</div>}

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : orders.length === 0 ? (
        <div className="empty-state">
          <EmptyBoxIllustration />
          <h3>No pending orders</h3>
          <p>New orders will show up here the moment a customer checks out.</p>
        </div>
      ) : (
        <div className="order-list">
          {orders.map((o) => (
            <div className="card merchant-order-card" key={o.orderId}>
              <div className="merchant-order-head">
                <div>
                  <div className="order-row-id">Order #{o.orderId.slice(0, 8)}</div>
                  <div className="muted">{formatDateTime(o.createdAt)}</div>
                </div>
                <StatusBadge status={o.status} />
              </div>
              <div className="merchant-order-actions">
                {o.status === 'PENDING_ACCEPTANCE' && (
                  <>
                    <button className="btn btn-secondary btn-sm" onClick={() => handleReject(o.orderId)} disabled={busyId === o.orderId}>
                      Reject
                    </button>
                    <button className="btn btn-primary btn-sm" onClick={() => act(o.orderId, acceptOrder)} disabled={busyId === o.orderId}>
                      {busyId === o.orderId ? <span className="spinner" /> : 'Accept'}
                    </button>
                  </>
                )}
                {o.status === 'ACCEPTED' && (
                  <button className="btn btn-primary btn-sm" onClick={() => act(o.orderId, readyOrder)} disabled={busyId === o.orderId}>
                    {busyId === o.orderId ? <span className="spinner" /> : 'Mark ready for pickup'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
