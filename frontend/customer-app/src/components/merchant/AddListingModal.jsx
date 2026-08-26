import { useState } from 'react';
import { searchProducts, requestProduct } from '../../api/catalog';
import { createListing } from '../../api/inventory';
import { uploadProductImage } from '../../api/upload';
import { ImageIcon } from '../Icons';

export default function AddListingModal({ onClose, onCreated }) {
  const [mode, setMode] = useState('find'); // find | request-sent
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState(null);
  const [price, setPrice] = useState('');
  const [stock, setStock] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const [requestForm, setRequestForm] = useState({ name: '', brand: '', category: '', description: '' });
  const [imageUrl, setImageUrl] = useState('');
  const [imageUploading, setImageUploading] = useState(false);
  const [imageError, setImageError] = useState('');

  async function handleImagePick(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setImageUploading(true);
    setImageError('');
    try {
      const url = await uploadProductImage(file);
      setImageUrl(url);
    } catch {
      setImageError('Could not upload this image — try again.');
    } finally {
      setImageUploading(false);
    }
  }

  async function handleSearch(e) {
    e.preventDefault();
    if (!query.trim()) return;
    setSearching(true);
    setError('');
    try {
      const data = await searchProducts({ q: query.trim() });
      setResults(data);
    } catch {
      setError('Search failed — try again.');
    } finally {
      setSearching(false);
    }
  }

  async function handleCreateListing(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await createListing({ productId: selected.id, price: Number(price), availableStock: Number(stock) });
      onCreated();
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not create this listing — you may already sell this product.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRequestProduct(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await requestProduct({ ...requestForm, images: imageUrl ? [imageUrl] : [], attributes: {} });
      setMode('request-sent');
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not submit this product request.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-sheet" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{mode === 'find' ? 'Add a listing' : mode === 'request' ? 'Request a new product' : 'Request submitted'}</h3>
          <button className="modal-close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {error && <div className="banner banner-error">{error}</div>}

        {mode === 'find' && !selected && (
          <>
            <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
              <input
                placeholder="Search the catalog, e.g. Amul milk"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                autoFocus
                style={{ flex: 1, border: '1.5px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '9px 12px', fontSize: 13.5, background: 'var(--bg)', color: 'var(--ink)' }}
              />
              <button className="btn btn-secondary btn-sm" disabled={searching}>{searching ? <span className="spinner" /> : 'Search'}</button>
            </form>

            {results.length > 0 && (
              <div className="pick-list">
                {results.map((p) => (
                  <button type="button" className="pick-row" key={p.id} onClick={() => setSelected(p)}>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: 13.5 }}>{p.name}</div>
                      <div className="muted">{p.brand} · {p.category}</div>
                    </div>
                  </button>
                ))}
              </div>
            )}

            <div className="auth-switch" style={{ marginTop: 16 }}>
              Can't find it? <button type="button" onClick={() => setMode('request')}>Request a new product</button>
            </div>
          </>
        )}

        {mode === 'find' && selected && (
          <form onSubmit={handleCreateListing}>
            <div className="banner banner-success" style={{ alignItems: 'center' }}>
              <span>{selected.name} — {selected.brand}</span>
            </div>
            <div className="field-row">
              <div className="field">
                <label htmlFor="np">Price (₹)</label>
                <input id="np" type="number" min="0" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required autoFocus />
              </div>
              <div className="field">
                <label htmlFor="ns">Stock</label>
                <input id="ns" type="number" min="0" value={stock} onChange={(e) => setStock(e.target.value)} required />
              </div>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setSelected(null)}>Back</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? <span className="spinner" /> : 'Create listing'}
              </button>
            </div>
          </form>
        )}

        {mode === 'request' && (
          <form onSubmit={handleRequestProduct}>
            <p style={{ fontSize: 12.5, color: 'var(--muted)', marginBottom: 14 }}>
              New products go through a quick admin review before you can list them.
            </p>
            <div className="field">
              <label htmlFor="rname">Product name</label>
              <input id="rname" value={requestForm.name} onChange={(e) => setRequestForm((f) => ({ ...f, name: e.target.value }))} required autoFocus />
            </div>
            <div className="field-row">
              <div className="field">
                <label htmlFor="rbrand">Brand</label>
                <input id="rbrand" value={requestForm.brand} onChange={(e) => setRequestForm((f) => ({ ...f, brand: e.target.value }))} required />
              </div>
              <div className="field">
                <label htmlFor="rcategory">Category</label>
                <input id="rcategory" value={requestForm.category} onChange={(e) => setRequestForm((f) => ({ ...f, category: e.target.value }))} required />
              </div>
            </div>
            <div className="field">
              <label htmlFor="rdesc">Description</label>
              <textarea id="rdesc" rows={2} value={requestForm.description} onChange={(e) => setRequestForm((f) => ({ ...f, description: e.target.value }))} />
            </div>
            <div className="field">
              <label htmlFor="rimage">Product photo (optional)</label>
              <label className="image-upload-drop" htmlFor="rimage">
                {imageUploading ? (
                  <span className="spinner" />
                ) : imageUrl ? (
                  <img src={imageUrl} alt="" className="image-upload-preview" />
                ) : (
                  <>
                    <ImageIcon style={{ width: 20, height: 20 }} />
                    <span>Upload a photo</span>
                  </>
                )}
              </label>
              <input id="rimage" type="file" accept="image/*" onChange={handleImagePick} style={{ display: 'none' }} />
              {imageError && <div className="banner banner-error" style={{ marginTop: 8, marginBottom: 0 }}>{imageError}</div>}
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setMode('find')}>Back</button>
              <button type="submit" className="btn btn-primary" disabled={submitting || imageUploading}>
                {submitting ? <span className="spinner" /> : 'Submit for review'}
              </button>
            </div>
          </form>
        )}

        {mode === 'request-sent' && (
          <div style={{ textAlign: 'center', padding: '10px 0' }}>
            <p style={{ fontSize: 13.5, marginBottom: 16 }}>
              Your request for "{requestForm.name}" was submitted. Once an admin approves it, come back here and search for it to create your listing.
            </p>
            <button className="btn btn-primary btn-block" onClick={onClose}>Done</button>
          </div>
        )}
      </div>
    </div>
  );
}
