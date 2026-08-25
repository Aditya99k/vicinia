import { useEffect, useState } from 'react';
import { myPayouts, mySettlements } from '../../api/settlement';
import StatusBadge from '../../components/StatusBadge';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { formatDateTime, formatMoney } from '../../utils/format';

export default function MerchantSettlementsPage() {
  const [entries, setEntries] = useState([]);
  const [payouts, setPayouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('entries');

  useEffect(() => {
    Promise.allSettled([mySettlements(), myPayouts()]).then(([e, p]) => {
      if (e.status === 'fulfilled') setEntries(e.value);
      if (p.status === 'fulfilled') setPayouts(p.value);
      setLoading(false);
    });
  }, []);

  const pendingTotal = entries.filter((e) => e.status === 'PENDING').reduce((s, e) => s + Number(e.net), 0);

  return (
    <div>
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Settlements</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Earnings from delivered orders and your payout history.</p>

      <div className="dashboard-stats" style={{ marginBottom: 20 }}>
        <div className="card stat-card no-hover">
          <div className="stat-icon"><span style={{ fontWeight: 800 }}>₹</span></div>
          <div>
            <div className="stat-value">{formatMoney(pendingTotal)}</div>
            <div className="muted">Pending settlement</div>
          </div>
        </div>
      </div>

      <div className="tab-row">
        <button className={tab === 'entries' ? 'active' : ''} onClick={() => setTab('entries')}>Entries</button>
        <button className={tab === 'payouts' ? 'active' : ''} onClick={() => setTab('payouts')}>Payouts</button>
      </div>

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : tab === 'entries' ? (
        entries.length === 0 ? (
          <div className="empty-state"><EmptyBoxIllustration /><h3>No settlement entries yet</h3><p>These appear once an order is delivered.</p></div>
        ) : (
          <div className="order-list">
            {entries.map((e) => (
              <div className="order-row card" key={e.id}>
                <div>
                  <div className="order-row-id">Order #{e.orderId.slice(0, 8)}</div>
                  <div className="muted">{formatDateTime(e.createdAt)} · gross {formatMoney(e.gross)} − commission {formatMoney(e.commission)}</div>
                </div>
                <div className="order-row-total">{formatMoney(e.net)}</div>
                <StatusBadge status={e.status} />
              </div>
            ))}
          </div>
        )
      ) : payouts.length === 0 ? (
        <div className="empty-state"><EmptyBoxIllustration /><h3>No payouts yet</h3><p>Payouts run daily, batching your settled entries.</p></div>
      ) : (
        <div className="order-list">
          {payouts.map((p) => (
            <div className="order-row card" key={p.id}>
              <div>
                <div className="order-row-id">Payout #{p.id.slice(0, 8)}</div>
                <div className="muted">{formatDateTime(p.createdAt)}</div>
              </div>
              <div className="order-row-total">{formatMoney(p.totalAmount)}</div>
              <StatusBadge status={p.status} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
