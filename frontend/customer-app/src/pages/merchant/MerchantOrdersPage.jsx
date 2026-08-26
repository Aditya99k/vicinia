import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { acceptOrder, getMe, pendingOrders, readyOrder, rejectOrder } from '../../api/merchant';
import { getOrderForMerchant } from '../../api/order';
import { useProductImages } from '../../hooks/useProductImages';
import StatusBadge from '../../components/StatusBadge';
import ProductImage from '../../components/ProductImage';
import { BellIcon } from '../../components/Icons';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { formatDateTime, formatMoney } from '../../utils/format';
import { useActionDialog } from '../../hooks/useActionDialog';

const POLL_MS = 8000;

export default function MerchantOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [details, setDetails] = useState({});
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');
  const [locationMissing, setLocationMissing] = useState(false);
  const [toast, setToast] = useState(null);
  const { promptText, dialog } = useActionDialog();

  const knownIds = useRef(null);
  const toastTimer = useRef(null);

  const load = useCallback((silent) => {
    if (!silent) setLoading(true);
    pendingOrders()
      .then((list) => {
        // First load just establishes the baseline — only orders that
        // arrive on a *later* poll count as "new" worth popping up for.
        if (knownIds.current) {
          const arrived = list.filter((o) => !knownIds.current.has(o.orderId));
          if (arrived.length > 0) {
            clearTimeout(toastTimer.current);
            setToast(arrived[0]);
            toastTimer.current = setTimeout(() => setToast(null), 6000);
          }
        }
        knownIds.current = new Set(list.map((o) => o.orderId));
        setOrders(list);

        list.forEach((o) => {
          if (o.status === 'PENDING_ACCEPTANCE' || o.status === 'ACCEPTED') {
            getOrderForMerchant(o.orderId)
              .then((full) => setDetails((d) => ({ ...d, [o.orderId]: full })))
              .catch(() => {});
          }
        });
      })
      .catch(() => { if (!silent) setError('Could not load your order queue.'); })
      .finally(() => { if (!silent) setLoading(false); });
  }, []);

  useEffect(() => {
    load(false);
    getMe().then((m) => setLocationMissing(m.latitude == null || m.longitude == null)).catch(() => {});
  }, [load]);

  useEffect(() => {
    const interval = setInterval(() => load(true), POLL_MS);
    return () => { clearInterval(interval); clearTimeout(toastTimer.current); };
  }, [load]);

  const allItems = Object.values(details).flatMap((o) => o.items);
  const { imageFor, categoryFor } = useProductImages(allItems.map((i) => i.productId));

  async function act(orderId, fn) {
    setBusyId(orderId);
    setError('');
    try {
      await fn(orderId);
      load(true);
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not update this order.');
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
      load(true);
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not reject this order.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      {dialog}
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Order queue</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Accept incoming orders and mark them ready for pickup.</p>

      {toast && (
        <Link to="#" onClick={(e) => e.preventDefault()} className="new-order-toast">
          <span className="new-order-toast-icon"><BellIcon style={{ width: 16, height: 16 }} /></span>
          <div>
            <div style={{ fontWeight: 700 }}>New order received</div>
            <div className="muted">Order #{toast.orderId.slice(0, 8)}</div>
          </div>
        </Link>
      )}

      {locationMissing && (
        <div className="banner banner-error">
          Your store has no location set — orders can be accepted, but marking one ready for pickup will fail
          until a rider can be found near you.{' '}
          <Link to="/merchant/store" style={{ fontWeight: 700 }}>Set your store location</Link>
        </div>
      )}

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
          {orders.map((o) => {
            const full = details[o.orderId];
            return (
              <div className="card merchant-order-card" key={o.orderId}>
                <div className="merchant-order-head">
                  <div>
                    <div className="order-row-id">Order #{o.orderId.slice(0, 8)}</div>
                    <div className="muted">{formatDateTime(o.createdAt)}</div>
                  </div>
                  <StatusBadge status={o.status} />
                </div>

                {full ? (
                  <div className="merchant-order-items">
                    {full.items.map((item) => (
                      <div className="merchant-order-item" key={item.listingId}>
                        <div className="order-item-thumb">
                          <ProductImage src={imageFor(item.productId)} name={item.productName} category={categoryFor(item.productId)} />
                        </div>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 700, fontSize: 13 }}>{item.productName}</div>
                          <div className="muted">{formatMoney(item.unitPrice)} × {item.quantity}</div>
                        </div>
                        <span style={{ fontWeight: 700, fontSize: 13 }}>{formatMoney(item.lineTotal)}</span>
                      </div>
                    ))}
                    <div className="merchant-order-total">
                      <span>Total</span>
                      <span>{formatMoney(full.totalAmount)}</span>
                    </div>
                  </div>
                ) : (
                  <div className="page-loading" style={{ padding: '10px 0' }}><span className="spinner" /></div>
                )}

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
            );
          })}
        </div>
      )}
    </div>
  );
}
