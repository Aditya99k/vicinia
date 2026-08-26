import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { myOrders } from '../api/order';
import { useProductImages } from '../hooks/useProductImages';
import ProductImage from '../components/ProductImage';
import StatusBadge from '../components/StatusBadge';
import { EmptyBoxIllustration } from '../components/Illustrations';
import { formatDateTime, formatMoney } from '../utils/format';

const POLL_MS = 10000;

export default function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    myOrders().then(setOrders).catch(() => setOrders([])).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, POLL_MS);
    return () => clearInterval(interval);
  }, [load]);

  const { imageFor, categoryFor } = useProductImages(orders.flatMap((o) => o.items.map((i) => i.productId)));

  if (loading) return <div className="page-loading"><span className="spinner" /> Loading…</div>;

  if (orders.length === 0) {
    return (
      <div className="empty-state">
        <EmptyBoxIllustration />
        <h3>No orders yet</h3>
        <p>Once you place an order, it'll show up here with live status tracking.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: 8 }}>Start shopping</Link>
      </div>
    );
  }

  return (
    <div>
      <h1 style={{ fontSize: 22, marginBottom: 18 }}>Your orders</h1>
      <div className="order-list">
        {orders.map((o) => (
          <Link to={`/orders/${o.id}`} className="order-row card" key={o.id}>
            <div className="order-row-thumbs">
              {o.items.slice(0, 3).map((item) => (
                <div className="order-row-thumb" key={item.listingId}>
                  <ProductImage src={imageFor(item.productId)} name={item.productName} category={categoryFor(item.productId)} />
                </div>
              ))}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="order-row-id">Order #{o.id.slice(0, 8)}</div>
              <div className="muted">{formatDateTime(o.createdAt)} · {o.items.length} item{o.items.length === 1 ? '' : 's'}</div>
            </div>
            <div className="order-row-total">{formatMoney(o.totalAmount)}</div>
            <StatusBadge status={o.status} />
          </Link>
        ))}
      </div>
    </div>
  );
}
