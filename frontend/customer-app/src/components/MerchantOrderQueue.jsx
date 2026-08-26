import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { acceptOrder, getMe, pendingOrders, readyOrder, rejectOrder } from '../api/merchant';
import { getOrderForMerchant } from '../api/order';
import { useProductImages } from '../hooks/useProductImages';
import StatusBadge from './StatusBadge';
import ProductImage from './ProductImage';
import { BellIcon, CheckCircleIcon, TruckIcon } from './Icons';
import { EmptyBoxIllustration } from './Illustrations';
import { formatDateTime, formatMoney } from '../utils/format';
import { useActionDialog } from '../hooks/useActionDialog';

const POLL_MS = 8000;
const DETAIL_STATUSES = new Set(['PENDING_ACCEPTANCE', 'ACCEPTED', 'READY']);

/** The merchant's live order queue — lives on the dashboard (their landing page) so incoming orders need no extra click to reach. */
export default function MerchantOrderQueue() {
  const [orders, setOrders] = useState([]);
  const [details, setDetails] = useState({});
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');
  const [locationMissing, setLocationMissing] = useState(false);
  const [toast, setToast] = useState(null);
  const [flashId, setFlashId] = useState(null);
  const { promptText, dialog } = useActionDialog();

  const knownIds = useRef(null);
  const toastTimer = useRef(null);
  const flashTimer = useRef(null);

  function showToast(icon, title, subtitle) {
    clearTimeout(toastTimer.current);
    setToast({ icon, title, subtitle });
    toastTimer.current = setTimeout(() => setToast(null), 4000);
  }

  function flash(orderId) {
    clearTimeout(flashTimer.current);
    setFlashId(orderId);
    flashTimer.current = setTimeout(() => setFlashId(null), 900);
  }

  const load = useCallback((silent) => {
    if (!silent) setLoading(true);
    pendingOrders()
      .then((list) => {
        // First load just establishes the baseline — only orders that
        // arrive on a *later* poll count as "new" worth popping up for.
        if (knownIds.current) {
          const arrived = list.filter((o) => !knownIds.current.has(o.orderId));
          if (arrived.length > 0) {
            showToast(<BellIcon style={{ width: 16, height: 16 }} />, 'New order received', `Order #${arrived[0].orderId.slice(0, 8)}`);
          }
        }
        knownIds.current = new Set(list.map((o) => o.orderId));
        setOrders(list);

        list.forEach((o) => {
          if (DETAIL_STATUSES.has(o.status)) {
            getOrderForMerchant(o.orderId)
              .then((full) => setDetails((d) => ({ ...d, [o.orderId]: full })))
              .catch(() => {});
          }
        });
      })
      .catch(() => { if (!silent) setError('Could not load your order queue.'); })
      .finally(() => { if (!silent) setLoading(false); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load(false);
    getMe().then((m) => setLocationMissing(m.latitude == null || m.longitude == null)).catch(() => {});
  }, [load]);

  useEffect(() => {
    const interval = setInterval(() => load(true), POLL_MS);
    return () => { clearInterval(interval); clearTimeout(toastTimer.current); clearTimeout(flashTimer.current); };
  }, [load]);

  const allItems = Object.values(details).flatMap((o) => o.items);
  const { imageFor, categoryFor } = useProductImages(allItems.map((i) => i.productId));

  async function act(orderId, fn, successTitle) {
    setBusyId(orderId);
    setError('');
    try {
      await fn(orderId);
      flash(orderId);
      showToast(<CheckCircleIcon style={{ width: 16, height: 16 }} />, successTitle, `Order #${orderId.slice(0, 8)}`);
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
      flash(orderId);
      showToast(<CheckCircleIcon style={{ width: 16, height: 16 }} />, 'Order rejected', `Order #${orderId.slice(0, 8)}`);
      load(true);
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not reject this order.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div id="order-queue">
      {dialog}
      <div className="section-title" style={{ marginTop: 28 }}><span>Order queue</span></div>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 14, marginTop: -8 }}>
        Accept incoming orders, mark them ready, and verify a rider's pickup against the order id below.
      </p>

      {toast && (
        <div className="new-order-toast">
          <span className="new-order-toast-icon">{toast.icon}</span>
          <div>
            <div style={{ fontWeight: 700 }}>{toast.title}</div>
            <div className="muted">{toast.subtitle}</div>
          </div>
        </div>
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
              <div className={`card merchant-order-card ${flashId === o.orderId ? 'action-flash' : ''} ${o.status === 'READY' ? 'awaiting-pickup' : ''}`} key={o.orderId}>
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
                      <button className="btn btn-primary btn-sm" onClick={() => act(o.orderId, acceptOrder, 'Order accepted')} disabled={busyId === o.orderId}>
                        {busyId === o.orderId ? <span className="spinner" /> : 'Accept'}
                      </button>
                    </>
                  )}
                  {o.status === 'ACCEPTED' && (
                    <button className="btn btn-primary btn-sm" onClick={() => act(o.orderId, readyOrder, 'Marked ready for pickup')} disabled={busyId === o.orderId}>
                      {busyId === o.orderId ? <span className="spinner" /> : 'Mark ready for pickup'}
                    </button>
                  )}
                  {o.status === 'READY' && (
                    <span className="pickup-waiting-badge">
                      <TruckIcon style={{ width: 14, height: 14 }} />
                      Awaiting rider pickup — verify against order #{o.orderId.slice(0, 8)}
                    </span>
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
