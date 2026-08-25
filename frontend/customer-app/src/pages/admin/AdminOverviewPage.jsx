import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminPending as adminPendingMerchants } from '../../api/merchant';
import { adminPendingProducts } from '../../api/catalog';
import { ChevronRightIcon, LayersIcon, StoreIcon, TicketIcon } from '../../components/Icons';
import { ShieldIllustration } from '../../components/Illustrations';

export default function AdminOverviewPage() {
  const [pendingMerchants, setPendingMerchants] = useState(0);
  const [pendingProducts, setPendingProducts] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([adminPendingMerchants(), adminPendingProducts()]).then(([m, p]) => {
      if (m.status === 'fulfilled') setPendingMerchants(m.value.length);
      if (p.status === 'fulfilled') setPendingProducts(p.value.length);
      setLoading(false);
    });
  }, []);

  return (
    <div>
      <div className="hero-card">
        <div className="copy">
          <span className="eyebrow">Admin console</span>
          <h1>Platform overview</h1>
          <p style={{ marginTop: 8, fontSize: 13.5, color: 'var(--muted)', maxWidth: '42ch' }}>
            Review merchant applications, moderate the catalog, and manage coupons and payouts.
          </p>
        </div>
        <ShieldIllustration style={{ width: 130, height: 'auto' }} />
      </div>

      <div className="dashboard-stats">
        <Link to="/admin/merchants" className="card stat-card">
          <div className="stat-icon"><StoreIcon style={{ width: 18, height: 18 }} /></div>
          <div>
            <div className="stat-value">{loading ? '—' : pendingMerchants}</div>
            <div className="muted">Merchant applications</div>
          </div>
          <ChevronRightIcon className="chev" style={{ width: 16, height: 16 }} />
        </Link>
        <Link to="/admin/products" className="card stat-card">
          <div className="stat-icon"><LayersIcon style={{ width: 18, height: 18 }} /></div>
          <div>
            <div className="stat-value">{loading ? '—' : pendingProducts}</div>
            <div className="muted">Products to review</div>
          </div>
          <ChevronRightIcon className="chev" style={{ width: 16, height: 16 }} />
        </Link>
        <Link to="/admin/coupons" className="card stat-card">
          <div className="stat-icon"><TicketIcon style={{ width: 18, height: 18 }} /></div>
          <div>
            <div className="stat-value">Manage</div>
            <div className="muted">Coupons</div>
          </div>
          <ChevronRightIcon className="chev" style={{ width: 16, height: 16 }} />
        </Link>
      </div>
    </div>
  );
}
