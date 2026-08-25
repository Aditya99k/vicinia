import { useEffect, useState } from 'react';
import { adminEntries, adminPayouts, adminRunBatch, adminRunProcessor } from '../../api/settlement';
import StatusBadge from '../../components/StatusBadge';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { formatDateTime, formatMoney } from '../../utils/format';

export default function AdminSettlementsPage() {
  const [entries, setEntries] = useState([]);
  const [payouts, setPayouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('entries');
  const [running, setRunning] = useState('');
  const [message, setMessage] = useState('');

  function load() {
    setLoading(true);
    Promise.allSettled([adminEntries(), adminPayouts()]).then(([e, p]) => {
      if (e.status === 'fulfilled') setEntries(e.value);
      if (p.status === 'fulfilled') setPayouts(p.value);
      setLoading(false);
    });
  }

  useEffect(load, []);

  async function handleRun(which) {
    setRunning(which);
    setMessage('');
    try {
      if (which === 'batch') await adminRunBatch();
      else await adminRunProcessor();
      setMessage(which === 'batch' ? 'Batch job run — pending entries grouped into payouts.' : 'Processor run — payouts advanced a step.');
      load();
    } catch {
      setMessage('Could not run this job.');
    } finally {
      setRunning('');
    }
  }

  return (
    <div>
      <div className="addresses-header">
        <div>
          <h1>Settlements</h1>
          <p>Platform-wide settlement ledger and merchant payouts.</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-secondary btn-sm" onClick={() => handleRun('batch')} disabled={running !== ''}>
            {running === 'batch' ? <span className="spinner" /> : 'Run payout batch'}
          </button>
          <button className="btn btn-primary btn-sm" onClick={() => handleRun('processor')} disabled={running !== ''}>
            {running === 'processor' ? <span className="spinner" /> : 'Run processor'}
          </button>
        </div>
      </div>

      {message && <div className="banner banner-success">{message}</div>}

      <div className="tab-row">
        <button className={tab === 'entries' ? 'active' : ''} onClick={() => setTab('entries')}>Entries</button>
        <button className={tab === 'payouts' ? 'active' : ''} onClick={() => setTab('payouts')}>Payouts</button>
      </div>

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : tab === 'entries' ? (
        entries.length === 0 ? (
          <div className="empty-state"><EmptyBoxIllustration /><h3>No settlement entries</h3></div>
        ) : (
          <div className="order-list">
            {entries.map((e) => (
              <div className="order-row card" key={e.id}>
                <div>
                  <div className="order-row-id">Order #{e.orderId.slice(0, 8)}</div>
                  <div className="muted">{formatDateTime(e.createdAt)} · merchant {e.merchantId.slice(0, 8)}</div>
                </div>
                <div className="order-row-total">{formatMoney(e.net)}</div>
                <StatusBadge status={e.status} />
              </div>
            ))}
          </div>
        )
      ) : payouts.length === 0 ? (
        <div className="empty-state"><EmptyBoxIllustration /><h3>No payouts</h3></div>
      ) : (
        <div className="order-list">
          {payouts.map((p) => (
            <div className="order-row card" key={p.id}>
              <div>
                <div className="order-row-id">Payout #{p.id.slice(0, 8)}</div>
                <div className="muted">{formatDateTime(p.createdAt)} · merchant {p.merchantId.slice(0, 8)}</div>
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
