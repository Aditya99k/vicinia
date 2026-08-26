import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useMerchantDirectory } from '../hooks/useMerchantDirectory';
import { useProductImages } from '../hooks/useProductImages';
import { listingsForMerchant } from '../api/inventory';
import { getMerchantStatus } from '../api/merchant';
import { AlertIcon, ArrowLeftIcon, ClockIcon, NavigationIcon, PackageIcon, StoreIcon } from '../components/Icons';
import ProductImage from '../components/ProductImage';
import QtyStepper from '../components/QtyStepper';
import { estimateDelivery } from '../utils/deliveryEstimate';
import { formatMoney } from '../utils/format';

export default function StorePage() {
  const { merchantId } = useParams();
  const { cart, addItem, updateItem, removeItem, clear } = useCart();
  const directory = useMerchantDirectory();
  const merchant = directory.get(merchantId);

  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addingId, setAddingId] = useState(null);
  const [busyListingId, setBusyListingId] = useState(null);
  const [conflict, setConflict] = useState(null);
  const [storeOpen, setStoreOpen] = useState(true);

  const { imageFor, categoryFor } = useProductImages(listings.map((l) => l.productId));

  useEffect(() => {
    setLoading(true);
    listingsForMerchant(merchantId)
      .then(setListings)
      .catch(() => setListings([]))
      .finally(() => setLoading(false));
    // Not the cached useMerchantDirectory — that's populated once per
    // session from /nearby (LIVE merchants only) and never re-checked, so
    // a store that closes after it's cached would otherwise show as open
    // here indefinitely. This is a fresh, authoritative check every visit.
    getMerchantStatus(merchantId).then((s) => setStoreOpen(s.open)).catch(() => {});
  }, [merchantId]);

  const addDisabled = !storeOpen;

  async function handleAdd(listing) {
    if (addDisabled) return;
    setAddingId(listing.id);
    setConflict(null);
    try {
      await addItem(listing.id, 1);
    } catch (err) {
      if (err?.response?.status === 409) setConflict(listing);
    } finally {
      setAddingId(null);
    }
  }

  async function handleSwitchStore() {
    if (!conflict) return;
    await clear();
    await handleAdd(conflict);
  }

  async function handleQty(listing, quantity) {
    setBusyListingId(listing.id);
    try {
      if (quantity <= 0) {
        await removeItem(listing.id);
      } else {
        await updateItem(listing.id, quantity);
      }
    } finally {
      setBusyListingId(null);
    }
  }

  if (loading) return <div className="page-loading"><span className="spinner" /> Loading…</div>;

  const { distanceLabel, etaLabel } = merchant ? estimateDelivery(merchant) : {};

  return (
    <div>
      <Link to="/" className="back-link"><ArrowLeftIcon style={{ width: 15, height: 15 }} /> Back to home</Link>

      <div className="store-header">
        <div className="store-header-icon"><StoreIcon /></div>
        <div>
          <h1>{merchant?.storeName || 'Store'}</h1>
          <div className="store-header-meta">
            {merchant?.city && <span className="item">{merchant.city}</span>}
            {distanceLabel && (
              <>
                <span className="dot" />
                <span className="item"><NavigationIcon /> {distanceLabel} away</span>
              </>
            )}
            {etaLabel && (
              <>
                <span className="dot" />
                <span className="item"><ClockIcon /> {etaLabel}</span>
              </>
            )}
            <span className="dot" />
            <span className="item"><PackageIcon /> {listings.length} item{listings.length === 1 ? '' : 's'}</span>
          </div>
        </div>
      </div>

      {!storeOpen && (
        <div className="banner banner-error">
          <AlertIcon style={{ width: 16, height: 16, flexShrink: 0, marginTop: 1 }} />
          <span>This shop is currently unavailable — it isn't accepting orders right now.</span>
        </div>
      )}

      {conflict && (
        <div className="banner banner-error">
          Your cart has items from another store — a Vicinia order comes from one merchant at a time.{' '}
          <button className="btn-ghost" style={{ padding: 0, display: 'inline', fontWeight: 700 }} onClick={handleSwitchStore}>
            Clear cart &amp; add this instead
          </button>
        </div>
      )}

      {listings.length === 0 ? (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>This store doesn't have any items listed yet.</p>
      ) : (
        <div className="product-grid">
          {listings.map((l) => {
            const cartQuantity = cart?.items?.find((i) => i.listingId === l.id)?.quantity || 0;
            return (
              <Link to={`/product/${l.productId}?listing=${l.id}`} className="product-card" key={l.id}>
                <div className="product-card-image">
                  <ProductImage src={imageFor(l.productId)} name={l.productName} category={categoryFor(l.productId) || l.productCategory} />
                </div>
                <div className="product-card-body">
                  <div className="brand">{l.productCategory}</div>
                  <div className="name">{l.productName}</div>
                </div>
                <div className="product-card-price-row">
                  <span className="price">{formatMoney(l.price)}</span>
                  {cartQuantity > 0 ? (
                    <QtyStepper
                      quantity={cartQuantity}
                      busy={busyListingId === l.id}
                      maxReached={addDisabled || cartQuantity >= l.availableStock}
                      onIncrement={() => !addDisabled && handleQty(l, cartQuantity + 1)}
                      onDecrement={() => handleQty(l, cartQuantity - 1)}
                    />
                  ) : (
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={(e) => { e.preventDefault(); e.stopPropagation(); handleAdd(l); }}
                      disabled={addDisabled || l.availableStock === 0 || addingId === l.id}
                    >
                      {addingId === l.id ? <span className="spinner" /> : addDisabled ? 'Unavailable' : l.availableStock === 0 ? 'Out of stock' : 'Add'}
                    </button>
                  )}
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
