import { useEffect, useState } from 'react';
import { adminApproveProduct, adminCreateCategory, adminCreateProduct, adminPendingProducts, adminRejectProduct, getCategories } from '../../api/catalog';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { PlusIcon } from '../../components/Icons';

export default function AdminProductsPage() {
  const [pending, setPending] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');

  const [newCategory, setNewCategory] = useState('');
  const [categoryBusy, setCategoryBusy] = useState(false);

  const [showProductForm, setShowProductForm] = useState(false);
  const [productForm, setProductForm] = useState({ name: '', brand: '', category: '', description: '' });
  const [productBusy, setProductBusy] = useState(false);
  const [productMessage, setProductMessage] = useState('');

  function load() {
    setLoading(true);
    Promise.allSettled([adminPendingProducts(), getCategories()]).then(([p, c]) => {
      if (p.status === 'fulfilled') setPending(p.value);
      if (c.status === 'fulfilled') setCategories(c.value);
      setLoading(false);
    });
  }

  useEffect(load, []);

  async function handleApprove(id) {
    setBusyId(id);
    setError('');
    try {
      await adminApproveProduct(id);
      load();
    } catch {
      setError('Could not approve this product.');
    } finally {
      setBusyId(null);
    }
  }

  async function handleReject(id) {
    const reason = window.prompt('Reason for rejecting this product?');
    if (reason === null) return;
    setBusyId(id);
    setError('');
    try {
      await adminRejectProduct(id, reason || 'Does not meet catalog standards');
      load();
    } catch {
      setError('Could not reject this product.');
    } finally {
      setBusyId(null);
    }
  }

  async function handleAddCategory(e) {
    e.preventDefault();
    if (!newCategory.trim()) return;
    setCategoryBusy(true);
    try {
      await adminCreateCategory(newCategory.trim());
      setNewCategory('');
      load();
    } catch {
      setError('Could not create this category — it may already exist.');
    } finally {
      setCategoryBusy(false);
    }
  }

  async function handleCreateProduct(e) {
    e.preventDefault();
    setProductBusy(true);
    setProductMessage('');
    try {
      await adminCreateProduct({ ...productForm, images: [], attributes: {} });
      setProductMessage(`"${productForm.name}" added to the catalog, live immediately.`);
      setProductForm({ name: '', brand: '', category: '', description: '' });
    } catch {
      setProductMessage('Could not create this product.');
    } finally {
      setProductBusy(false);
    }
  }

  return (
    <div>
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Catalog moderation</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Review merchant-submitted products and manage categories.</p>

      {error && <div className="banner banner-error">{error}</div>}

      <div className="section-title"><span>Pending review</span></div>
      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : pending.length === 0 ? (
        <div className="empty-state" style={{ padding: '20px 0' }}>
          <EmptyBoxIllustration />
          <h3>Nothing pending</h3>
          <p>Merchant-requested products awaiting review will appear here.</p>
        </div>
      ) : (
        <div className="order-list" style={{ marginBottom: 28 }}>
          {pending.map((p) => (
            <div className="card merchant-order-card" key={p.id}>
              <div className="merchant-order-head">
                <div>
                  <div className="order-row-id">{p.name}</div>
                  <div className="muted">{p.brand} · {p.category}</div>
                  {p.description && <div className="muted" style={{ marginTop: 4 }}>{p.description}</div>}
                </div>
              </div>
              <div className="merchant-order-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleReject(p.id)} disabled={busyId === p.id}>Reject</button>
                <button className="btn btn-primary btn-sm" onClick={() => handleApprove(p.id)} disabled={busyId === p.id}>
                  {busyId === p.id ? <span className="spinner" /> : 'Approve'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="section-title"><span>Categories</span></div>
      <div className="card" style={{ marginBottom: 20 }}>
        <div className="chip-row" style={{ marginBottom: 14 }}>
          {categories.map((c) => <span key={c.id} className="badge badge-muted">{c.name}</span>)}
        </div>
        <form onSubmit={handleAddCategory} style={{ display: 'flex', gap: 8 }}>
          <input
            placeholder="New category name"
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            style={{ flex: 1, border: '1.5px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '9px 12px', fontSize: 13.5, background: 'var(--bg)', color: 'var(--ink)' }}
          />
          <button className="btn btn-secondary btn-sm" disabled={categoryBusy}>{categoryBusy ? <span className="spinner" /> : 'Add'}</button>
        </form>
      </div>

      <div className="section-title">
        <span>Add a product directly</span>
        <button onClick={() => setShowProductForm((s) => !s)}>{showProductForm ? 'Hide' : 'Show'}</button>
      </div>
      {showProductForm && (
        <form onSubmit={handleCreateProduct} className="card">
          {productMessage && <div className="banner banner-success">{productMessage}</div>}
          <div className="field">
            <label htmlFor="pname">Name</label>
            <input id="pname" value={productForm.name} onChange={(e) => setProductForm((f) => ({ ...f, name: e.target.value }))} required />
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="pbrand">Brand</label>
              <input id="pbrand" value={productForm.brand} onChange={(e) => setProductForm((f) => ({ ...f, brand: e.target.value }))} required />
            </div>
            <div className="field">
              <label htmlFor="pcategory">Category</label>
              <input id="pcategory" list="category-list" value={productForm.category} onChange={(e) => setProductForm((f) => ({ ...f, category: e.target.value }))} required />
              <datalist id="category-list">
                {categories.map((c) => <option key={c.id} value={c.name} />)}
              </datalist>
            </div>
          </div>
          <div className="field">
            <label htmlFor="pdesc">Description</label>
            <textarea id="pdesc" rows={2} value={productForm.description} onChange={(e) => setProductForm((f) => ({ ...f, description: e.target.value }))} />
          </div>
          <button className="btn btn-primary btn-block" disabled={productBusy}>
            {productBusy ? <span className="spinner" /> : <><PlusIcon style={{ width: 15, height: 15 }} /> Add product</>}
          </button>
        </form>
      )}
    </div>
  );
}
