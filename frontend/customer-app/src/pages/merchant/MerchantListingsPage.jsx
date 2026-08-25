import { useEffect, useState } from 'react';
import { myListings, updateListing } from '../../api/inventory';
import ListingEditModal from '../../components/merchant/ListingEditModal';
import AddListingModal from '../../components/merchant/AddListingModal';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { PlusIcon } from '../../components/Icons';
import { formatMoney } from '../../utils/format';

export default function MerchantListingsPage() {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [adding, setAdding] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  function load() {
    setLoading(true);
    myListings().then(setListings).catch(() => setListings([])).finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleEditSubmit(payload) {
    setSubmitting(true);
    setFormError('');
    try {
      await updateListing(editing.id, payload);
      setEditing(null);
      load();
    } catch {
      setFormError('Could not save this listing.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="addresses-header">
        <div>
          <h1>Listings</h1>
          <p>Manage what you sell and how much stock you have.</p>
        </div>
        <button className="btn btn-primary btn-sm" onClick={() => setAdding(true)}>
          <PlusIcon style={{ width: 15, height: 15 }} /> Add listing
        </button>
      </div>

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : listings.length === 0 ? (
        <div className="empty-state">
          <EmptyBoxIllustration />
          <h3>No listings yet</h3>
          <p>Add your first product listing to start selling on Vicinia.</p>
          <button className="btn btn-primary" style={{ marginTop: 8 }} onClick={() => setAdding(true)}>
            <PlusIcon style={{ width: 16, height: 16 }} /> Add listing
          </button>
        </div>
      ) : (
        <div className="listing-table">
          {listings.map((l) => (
            <button className="listing-table-row" key={l.id} onClick={() => setEditing(l)}>
              <div className="name-cell">
                <div className="name">{l.productName}</div>
                <div className="muted">{l.productCategory}</div>
              </div>
              <div className="price-cell">{formatMoney(l.price)}</div>
              <div className={`stock-cell ${l.availableStock === 0 ? 'zero' : ''}`}>{l.availableStock} in stock</div>
              <span className={`badge ${l.active ? 'badge-success' : 'badge-muted'}`}>{l.active ? 'Active' : 'Hidden'}</span>
            </button>
          ))}
        </div>
      )}

      {editing && (
        <ListingEditModal
          listing={editing}
          onClose={() => setEditing(null)}
          onSubmit={handleEditSubmit}
          submitting={submitting}
          error={formError}
        />
      )}

      {adding && (
        <AddListingModal
          onClose={() => setAdding(false)}
          onCreated={() => { setAdding(false); load(); }}
        />
      )}
    </div>
  );
}
