import { useEffect, useState } from 'react';
import { adminApprove, adminPending, adminReinstate, adminReject, adminSuspend } from '../../api/merchant';
import { EmptyBoxIllustration } from '../../components/Illustrations';

export default function AdminMerchantsPage() {
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');
  const [utilId, setUtilId] = useState('');
  const [utilBusy, setUtilBusy] = useState(false);
  const [utilMessage, setUtilMessage] = useState('');

  function load() {
    setLoading(true);
    adminPending().then(setPending).catch(() => setError('Could not load pending applications.')).finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleApprove(id) {
    setBusyId(id);
    setError('');
    try {
      await adminApprove(id);
      load();
    } catch {
      setError('Could not approve this merchant.');
    } finally {
      setBusyId(null);
    }
  }

  async function handleReject(id) {
    const reason = window.prompt('Reason for rejecting this application?');
    if (reason === null) return;
    setBusyId(id);
    setError('');
    try {
      await adminReject(id, reason || 'Application incomplete');
      load();
    } catch {
      setError('Could not reject this merchant.');
    } finally {
      setBusyId(null);
    }
  }

  async function handleUtil(action) {
    if (!utilId.trim()) return;
    setUtilBusy(true);
    setUtilMessage('');
    try {
      if (action === 'suspend') {
        const reason = window.prompt('Reason for suspending this merchant?') || 'Policy violation';
        await adminSuspend(utilId.trim(), reason);
      } else {
        await adminReinstate(utilId.trim());
      }
      setUtilMessage(`Merchant ${action === 'suspend' ? 'suspended' : 'reinstated'}.`);
    } catch {
      setUtilMessage('Could not complete this action — check the merchant ID.');
    } finally {
      setUtilBusy(false);
    }
  }

  return (
    <div>
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Merchant applications</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Review and approve stores waiting to join Vicinia.</p>

      {error && <div className="banner banner-error">{error}</div>}

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : pending.length === 0 ? (
        <div className="empty-state">
          <EmptyBoxIllustration />
          <h3>No pending applications</h3>
          <p>New merchant applications will appear here for review.</p>
        </div>
      ) : (
        <div className="order-list" style={{ marginBottom: 28 }}>
          {pending.map((m) => (
            <div className="card merchant-order-card" key={m.id}>
              <div className="merchant-order-head">
                <div>
                  <div className="order-row-id">{m.storeName}</div>
                  <div className="muted">{m.addressLine1}, {m.city}, {m.state} — {m.pincode}</div>
                  {m.description && <div className="muted" style={{ marginTop: 4 }}>{m.description}</div>}
                  {m.documentTypes?.length > 0 && (
                    <div className="chip-row" style={{ marginTop: 8 }}>
                      {m.documentTypes.map((d) => <span key={d} className="badge badge-muted">{d}</span>)}
                    </div>
                  )}
                </div>
              </div>
              <div className="merchant-order-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleReject(m.id)} disabled={busyId === m.id}>Reject</button>
                <button className="btn btn-primary btn-sm" onClick={() => handleApprove(m.id)} disabled={busyId === m.id}>
                  {busyId === m.id ? <span className="spinner" /> : 'Approve'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="section-title"><span>Suspend or reinstate</span></div>
      <div className="card">
        <p style={{ fontSize: 12.5, color: 'var(--muted)', marginBottom: 12 }}>
          There's no directory of live merchants yet — paste a merchant ID (from support or the applications above) to act on it directly.
        </p>
        {utilMessage && <div className="banner banner-success">{utilMessage}</div>}
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            placeholder="Merchant ID"
            value={utilId}
            onChange={(e) => setUtilId(e.target.value)}
            style={{ flex: 1, border: '1.5px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '9px 12px', fontSize: 13.5, background: 'var(--bg)', color: 'var(--ink)' }}
          />
          <button className="btn btn-secondary btn-sm" onClick={() => handleUtil('suspend')} disabled={utilBusy}>Suspend</button>
          <button className="btn btn-primary btn-sm" onClick={() => handleUtil('reinstate')} disabled={utilBusy}>Reinstate</button>
        </div>
      </div>
    </div>
  );
}
