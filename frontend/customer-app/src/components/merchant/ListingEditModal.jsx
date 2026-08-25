import { useState } from 'react';

export default function ListingEditModal({ listing, onClose, onSubmit, submitting, error }) {
  const [price, setPrice] = useState(listing.price);
  const [availableStock, setAvailableStock] = useState(listing.availableStock);
  const [active, setActive] = useState(listing.active);

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit({ price: Number(price), availableStock: Number(availableStock), active });
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-sheet" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{listing.productName}</h3>
          <button className="modal-close" onClick={onClose} aria-label="Close">✕</button>
        </div>
        {error && <div className="banner banner-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="field-row">
            <div className="field">
              <label htmlFor="price">Price (₹)</label>
              <input id="price" type="number" min="0" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="stock">Available stock</label>
              <input id="stock" type="number" min="0" value={availableStock} onChange={(e) => setAvailableStock(e.target.value)} required />
            </div>
          </div>
          <div className="checkbox-row">
            <input id="active" type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            <label htmlFor="active">Listed and visible to customers</label>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? <span className="spinner" /> : 'Save changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
