import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { cancelOrder, getOrder } from '../api/order';
import { createReview } from '../api/review';
import { useProductImages } from '../hooks/useProductImages';
import { ArrowLeftIcon, CheckCircleIcon, PackageIcon, StarIcon, TruckIcon } from '../components/Icons';
import StatusBadge from '../components/StatusBadge';
import ShopBanner from '../components/ShopBanner';
import ProductImage from '../components/ProductImage';
import { useActionDialog } from '../hooks/useActionDialog';
import { formatDateTime, formatMoney } from '../utils/format';

const POLL_MS = 8000;
// Must match order-service's real OrderStatus lifecycle exactly (see that
// enum's own javadoc) — DELIVERY_ASSIGNED sits between READY_FOR_PICKUP and
// OUT_FOR_DELIVERY. Missing it here previously meant the timeline's
// findIndex() returned -1 the moment a rider was assigned (every step
// showing "not done"), and polling stopped outright since this same set
// gates it — a status the poll never expects to see again is where it
// silently gives up.
const LIVE_STATUSES = new Set(['CREATED', 'PAYMENT_PENDING', 'CONFIRMED', 'MERCHANT_ACCEPTED', 'PREPARING', 'READY_FOR_PICKUP', 'DELIVERY_ASSIGNED', 'OUT_FOR_DELIVERY']);

const HAPPY_PATH = [
  { status: 'CONFIRMED', label: 'Order confirmed', icon: CheckCircleIcon },
  { status: 'MERCHANT_ACCEPTED', label: 'Store accepted', icon: PackageIcon },
  { status: 'PREPARING', label: 'Preparing your order', icon: PackageIcon },
  { status: 'READY_FOR_PICKUP', label: 'Ready for pickup', icon: PackageIcon },
  { status: 'DELIVERY_ASSIGNED', label: 'Delivery partner assigned', icon: TruckIcon },
  { status: 'OUT_FOR_DELIVERY', label: 'Out for delivery', icon: TruckIcon },
  { status: 'DELIVERED', label: 'Delivered', icon: CheckCircleIcon },
];
const TERMINAL_BAD = new Set(['PAYMENT_FAILED', 'CANCELLED', 'MERCHANT_REJECTED', 'REFUND_PENDING', 'REFUNDED']);
const CANCELLABLE = new Set(['CREATED', 'PAYMENT_PENDING', 'CONFIRMED', 'MERCHANT_ACCEPTED']);

export default function OrderDetailPage() {
  const { id } = useParams();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState('');
  const [reviewing, setReviewing] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [reviewedIds, setReviewedIds] = useState(new Set());
  const { confirm, dialog } = useActionDialog();

  const load = useCallback((silent) => {
    if (!silent) setLoading(true);
    getOrder(id)
      .then(setOrder)
      .catch(() => { if (!silent) setError('Could not load this order.'); })
      .finally(() => { if (!silent) setLoading(false); });
  }, [id]);

  useEffect(() => {
    load(false);
  }, [load]);

  // Polls while the order is still moving — stops once it lands on a
  // terminal status, so a delivered/cancelled order doesn't keep pinging.
  useEffect(() => {
    if (!order || !LIVE_STATUSES.has(order.status)) return;
    const interval = setInterval(() => load(true), POLL_MS);
    return () => clearInterval(interval);
  }, [order, load]);

  const { imageFor, categoryFor } = useProductImages((order?.items || []).map((i) => i.productId));

  async function handleCancel() {
    if (!(await confirm('Cancel this order?', { title: 'Cancel order', danger: true, confirmLabel: 'Cancel order' }))) return;
    setCancelling(true);
    try {
      await cancelOrder(id, 'Changed my mind');
      load();
    } catch {
      setError('Could not cancel this order.');
    } finally {
      setCancelling(false);
    }
  }

  async function submitReview(productId) {
    setReviewSubmitting(true);
    try {
      await createReview({ productId, rating: reviewRating, comment: reviewComment });
      setReviewedIds((s) => new Set(s).add(productId));
      setReviewing(null);
      setReviewComment('');
      setReviewRating(5);
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not submit your review.');
    } finally {
      setReviewSubmitting(false);
    }
  }

  if (loading) return <div className="page-loading"><span className="spinner" /> Loading…</div>;
  if (!order) return <div className="empty-state"><h3>Order not found</h3></div>;

  const stepIndex = HAPPY_PATH.findIndex((s) => s.status === order.status);
  const isBad = TERMINAL_BAD.has(order.status);

  return (
    <div className="order-detail">
      {dialog}
      <Link to="/orders" className="back-link"><ArrowLeftIcon style={{ width: 15, height: 15 }} /> Back to orders</Link>

      <div className="addresses-header">
        <div>
          <h1>Order #{order.id.slice(0, 8)}</h1>
          <p>{formatDateTime(order.createdAt)}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>
      <div style={{ marginBottom: 16 }}><ShopBanner merchantId={order.merchantId} /></div>

      {error && <div className="banner banner-error">{error}</div>}

      {isBad ? (
        <div className="card" style={{ marginBottom: 20 }}>
          <p style={{ fontSize: 13.5 }}>
            {order.status === 'CANCELLED' && 'This order was cancelled.'}
            {order.status === 'MERCHANT_REJECTED' && 'The store was unable to fulfil this order.'}
            {order.status === 'PAYMENT_FAILED' && 'Payment for this order failed.'}
            {(order.status === 'REFUND_PENDING' || order.status === 'REFUNDED') && 'This order was refunded.'}
            {order.cancellationReason && <span> — {order.cancellationReason}</span>}
          </p>
        </div>
      ) : (
        <div className="timeline card">
          {HAPPY_PATH.map((step, i) => {
            const Icon = step.icon;
            const done = i <= stepIndex;
            return (
              <div className={`timeline-step ${done ? 'done' : ''}`} key={step.status}>
                <div className="timeline-dot"><Icon style={{ width: 15, height: 15 }} /></div>
                {i < HAPPY_PATH.length - 1 && <div className="timeline-line" />}
                <span>{step.label}</span>
              </div>
            );
          })}
        </div>
      )}

      <div className="section-title"><span>Items</span></div>
      <div className="card" style={{ marginBottom: 16 }}>
        {order.items.map((item) => (
          <div className="order-item-row" key={item.listingId}>
            <div className="order-item-thumb">
              <ProductImage src={imageFor(item.productId)} name={item.productName} category={categoryFor(item.productId)} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, fontSize: 13.5 }}>{item.productName}</div>
              <div className="muted">{formatMoney(item.unitPrice)} × {item.quantity}</div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontWeight: 700 }}>{formatMoney(item.lineTotal)}</span>
              {order.status === 'DELIVERED' && !reviewedIds.has(item.productId) && (
                <button className="btn-ghost" onClick={() => setReviewing(reviewing === item.productId ? null : item.productId)}>
                  Rate it
                </button>
              )}
            </div>
          </div>
        ))}

        {reviewing && (
          <div className="review-form">
            <div className="stars-input">
              {Array.from({ length: 5 }).map((_, i) => (
                <button key={i} type="button" onClick={() => setReviewRating(i + 1)}>
                  <StarIcon style={{ width: 20, height: 20, color: i < reviewRating ? 'var(--warn)' : 'var(--line)' }} />
                </button>
              ))}
            </div>
            <textarea
              rows={2}
              placeholder="Share your thoughts (optional)"
              value={reviewComment}
              onChange={(e) => setReviewComment(e.target.value)}
            />
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-secondary btn-sm" onClick={() => setReviewing(null)}>Cancel</button>
              <button className="btn btn-primary btn-sm" disabled={reviewSubmitting} onClick={() => submitReview(reviewing)}>
                {reviewSubmitting ? <span className="spinner" /> : 'Submit review'}
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="card summary-card">
        <div className="summary-row"><span>Subtotal</span><span>{formatMoney(order.subtotal)}</span></div>
        {order.discountAmount > 0 && (
          <div className="summary-row discount"><span>Coupon ({order.couponCode})</span><span>−{formatMoney(order.discountAmount)}</span></div>
        )}
        <div className="summary-row total"><span>Total</span><span>{formatMoney(order.totalAmount)}</span></div>
      </div>

      {CANCELLABLE.has(order.status) && (
        <button className="btn btn-secondary btn-block" style={{ marginTop: 16, color: 'var(--danger)' }} onClick={handleCancel} disabled={cancelling}>
          {cancelling ? <span className="spinner" /> : 'Cancel order'}
        </button>
      )}
    </div>
  );
}
