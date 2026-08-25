import { useEffect, useState } from 'react';
import { adminAll, adminApprove, adminPending, adminReinstate, adminReject, adminSuspend } from '../../api/merchant';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import StatusBadge from '../../components/StatusBadge';
import { useActionDialog } from '../../hooks/useActionDialog';
import { formatDate } from '../../utils/format';

export default function AdminMerchantsPage() {
  const [tab, setTab] = useState('pending');
  const [pending, setPending] = useState([]);
  const [all, setAll] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');
  const [utilId, setUtilId] = useState('');
  const [utilBusy, setUtilBusy] = useState(false);
  const [utilMessage, setUtilMessage] = useState('');
  const { confirm, promptText, dialog } = useActionDialog();

  function load() {
    setLoading(true);
    Promise.allSettled([adminPending(), adminAll()]).then(([p, a]) => {
      if (p.status === 'fulfilled') setPending(p.value);
      else setError('Could not load pending applications.');
      if (a.status === 'fulfilled') setAll(a.value);
      setLoading(false);
    });
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
    const reason = await promptText('Reason for rejecting this application?', { title: 'Reject application', placeholder: 'e.g. Documents unclear' });
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
        const reason = await promptText('Reason for suspending this merchant?', { title: 'Suspend merchant', placeholder: 'e.g. Policy violation' });
        if (reason === null) { setUtilBusy(false); return; }
        await adminSuspend(utilId.trim(), reason || 'Policy violation');
      } else {
        if (!(await confirm('Reinstate this merchant?', { title: 'Reinstate merchant' }))) { setUtilBusy(false); return; }
        await adminReinstate(utilId.trim());
      }
      setUtilMessage(`Merchant ${action === 'suspend' ? 'suspended' : 'reinstated'}.`);
      load();
    } catch {
      setUtilMessage('Could not complete this action — check the merchant ID.');
    } finally {
      setUtilBusy(false);
    }
  }

  return (
    <div>
      {dialog}
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Merchants</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Review new applications and keep a record of every merchant you've reviewed.</p>

      {error && <div className="banner banner-error">{error}</div>}

      <div className="tab-row">
        <button className={tab === 'pending' ? 'active' : ''} onClick={() => setTab('pending')}>
          Pending {pending.length > 0 && `(${pending.length})`}
        </button>
        <button className={tab === 'all' ? 'active' : ''} onClick={() => setTab('all')}>All merchants</button>
      </div>

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : tab === 'pending' ? (
        pending.length === 0 ? (
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
        )
      ) : all.length === 0 ? (
        <div className="empty-state"><EmptyBoxIllustration /><h3>No merchants yet</h3></div>
      ) : (
        <div className="listing-table" style={{ marginBottom: 28 }}>
          {all.map((m) => (
            <div className="listing-table-row" key={m.id} style={{ cursor: 'default' }}>
              <div className="name-cell">
                <div className="name">{m.storeName}</div>
                <div className="muted">{m.city}, {m.state}</div>
              </div>
              <div className="stock-cell muted">Applied {formatDate(m.createdAt) || '—'}</div>
              <StatusBadge status={m.status} />
            </div>
          ))}
        </div>
      )}

      <div className="section-title"><span>Suspend or reinstate</span></div>
      <div className="card">
        <p style={{ fontSize: 12.5, color: 'var(--muted)', marginBottom: 12 }}>
          Paste a merchant ID (from the list above) to act on it directly.
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
