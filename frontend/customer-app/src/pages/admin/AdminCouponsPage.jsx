import { useEffect, useState } from 'react';
import { adminCreateCoupon, adminListCoupons, adminUpdateCoupon } from '../../api/coupon';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { PlusIcon, TicketIcon } from '../../components/Icons';
import { formatMoney } from '../../utils/format';

const emptyForm = {
  code: '', description: '', discountType: 'PERCENTAGE', discountValue: '',
  maxDiscountAmount: '', minOrderValue: '', usageLimit: '', perUserLimit: 1,
};

export default function AdminCouponsPage() {
  const [coupons, setCoupons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  function load() {
    setLoading(true);
    adminListCoupons().then(setCoupons).catch(() => setCoupons([])).finally(() => setLoading(false));
  }

  useEffect(load, []);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleCreate(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await adminCreateCoupon({
        code: form.code.toUpperCase(),
        description: form.description,
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        maxDiscountAmount: form.maxDiscountAmount ? Number(form.maxDiscountAmount) : null,
        minOrderValue: form.minOrderValue ? Number(form.minOrderValue) : null,
        usageLimit: form.usageLimit ? Number(form.usageLimit) : null,
        perUserLimit: Number(form.perUserLimit),
        validFrom: null,
        validUntil: null,
      });
      setForm(emptyForm);
      setShowForm(false);
      load();
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not create this coupon — the code may already exist.');
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleActive(coupon) {
    setBusyId(coupon.id);
    try {
      await adminUpdateCoupon(coupon.id, { active: !coupon.active });
      load();
    } catch {
      setError('Could not update this coupon.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <div className="addresses-header">
        <div>
          <h1>Coupons</h1>
          <p>Create and manage platform-wide discount codes.</p>
        </div>
        <button className="btn btn-primary btn-sm" onClick={() => setShowForm((s) => !s)}>
          <PlusIcon style={{ width: 15, height: 15 }} /> New coupon
        </button>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {showForm && (
        <form onSubmit={handleCreate} className="card" style={{ marginBottom: 20 }}>
          <div className="field-row">
            <div className="field">
              <label htmlFor="code">Code</label>
              <input id="code" value={form.code} onChange={(e) => set('code', e.target.value)} required autoFocus />
            </div>
            <div className="field">
              <label htmlFor="dtype">Type</label>
              <select id="dtype" value={form.discountType} onChange={(e) => set('discountType', e.target.value)} style={{ border: '1.5px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '9px 12px', fontSize: 13.5, background: 'var(--bg)', color: 'var(--ink)' }}>
                <option value="PERCENTAGE">Percentage</option>
                <option value="FLAT">Flat amount</option>
              </select>
            </div>
          </div>
          <div className="field">
            <label htmlFor="desc">Description</label>
            <input id="desc" value={form.description} onChange={(e) => set('description', e.target.value)} required />
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="dvalue">{form.discountType === 'PERCENTAGE' ? 'Percent off' : 'Amount off (₹)'}</label>
              <input id="dvalue" type="number" min="0" step="0.01" value={form.discountValue} onChange={(e) => set('discountValue', e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="maxd">Max discount (₹, optional)</label>
              <input id="maxd" type="number" min="0" step="0.01" value={form.maxDiscountAmount} onChange={(e) => set('maxDiscountAmount', e.target.value)} />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="minv">Min order value (₹, optional)</label>
              <input id="minv" type="number" min="0" step="0.01" value={form.minOrderValue} onChange={(e) => set('minOrderValue', e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor="ulimit">Total usage limit (optional)</label>
              <input id="ulimit" type="number" min="1" value={form.usageLimit} onChange={(e) => set('usageLimit', e.target.value)} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="perUser">Per-user limit</label>
            <input id="perUser" type="number" min="1" value={form.perUserLimit} onChange={(e) => set('perUserLimit', e.target.value)} required />
          </div>
          <button className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? <span className="spinner" /> : 'Create coupon'}
          </button>
        </form>
      )}

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : coupons.length === 0 ? (
        <div className="empty-state"><EmptyBoxIllustration /><h3>No coupons yet</h3><p>Create your first discount code above.</p></div>
      ) : (
        <div className="listing-table">
          {coupons.map((c) => (
            <div className="listing-table-row" key={c.id} style={{ cursor: 'default' }}>
              <div className="name-cell">
                <div className="name"><TicketIcon style={{ width: 13, height: 13, marginRight: 5, verticalAlign: -2 }} />{c.code}</div>
                <div className="muted">{c.description}</div>
              </div>
              <div className="price-cell">
                {c.discountType === 'PERCENTAGE' ? `${c.discountValue}% off` : `${formatMoney(c.discountValue)} off`}
              </div>
              <div className="stock-cell muted">{c.minOrderValue ? `Min ${formatMoney(c.minOrderValue)}` : 'No minimum'}</div>
              <button
                className={`badge ${c.active ? 'badge-success' : 'badge-muted'}`}
                style={{ border: 'none', cursor: 'pointer' }}
                onClick={() => toggleActive(c)}
                disabled={busyId === c.id}
              >
                {busyId === c.id ? <span className="spinner" /> : c.active ? 'Active' : 'Inactive'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
