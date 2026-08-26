import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { getProduct } from '../api/catalog';
import { listingsForProduct } from '../api/inventory';
import { productRating, productReviews } from '../api/review';
import { useCart } from '../context/CartContext';
import { useMerchantDirectory } from '../hooks/useMerchantDirectory';
import { ArrowLeftIcon, StarIcon, StoreIcon } from '../components/Icons';
import ProductImage from '../components/ProductImage';
import QtyStepper from '../components/QtyStepper';
import { formatMoney, formatDate } from '../utils/format';

export default function ProductPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const highlightedListingId = searchParams.get('listing');
  const { cart, addItem, updateItem, removeItem, clear } = useCart();
  const directory = useMerchantDirectory();

  const [product, setProduct] = useState(null);
  const [listings, setListings] = useState([]);
  const [rating, setRating] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [addingId, setAddingId] = useState(null);
  const [busyListingId, setBusyListingId] = useState(null);
  const [conflict, setConflict] = useState(null);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      getProduct(id),
      listingsForProduct(id),
      productRating(id),
      productReviews(id),
    ]).then(([p, l, r, rv]) => {
      if (p.status === 'fulfilled') setProduct(p.value);
      if (l.status === 'fulfilled') {
        const active = l.value.filter((x) => x.active);
        // A specific listing (e.g. clicked from a store's own tile on the home
        // page) sorts first, so "Add to cart" naturally adds the item the
        // customer actually meant, not just whichever offer the API listed first.
        active.sort((a, b) => (a.id === highlightedListingId ? -1 : b.id === highlightedListingId ? 1 : 0));
        setListings(active);
      }
      if (r.status === 'fulfilled') setRating(r.value);
      if (rv.status === 'fulfilled') setReviews(rv.value);
    }).finally(() => setLoading(false));
  }, [id, highlightedListingId]);

  async function handleAdd(listing) {
    setAddingId(listing.id);
    setError('');
    setConflict(null);
    try {
      await addItem(listing.id, 1);
    } catch (err) {
      if (err?.response?.status === 409) {
        setConflict(listing);
      } else {
        setError('Could not add this item to your cart.');
      }
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
  if (!product) return <div className="empty-state"><h3>Product not found</h3></div>;

  return (
    <div className="product-detail">
      <Link to="/search" className="back-link"><ArrowLeftIcon style={{ width: 15, height: 15 }} /> Back to results</Link>

      <div className="product-detail-grid">
        <div className="product-detail-image">
          <ProductImage src={product.images?.[0]} name={product.name} category={product.category} large />
        </div>

        <div>
          <div className="category-tag">{product.category}</div>
          <h1 style={{ fontSize: 24, marginTop: 6 }}>{product.name}</h1>
          <p style={{ color: 'var(--muted)', fontSize: 13, marginTop: 2 }}>{product.brand}</p>

          {rating && rating.reviewCount > 0 && (
            <div className="rating-row">
              <StarIcon style={{ width: 15, height: 15, color: 'var(--warn)' }} />
              <strong>{rating.averageRating.toFixed(1)}</strong>
              <span className="muted">({rating.reviewCount} review{rating.reviewCount === 1 ? '' : 's'})</span>
            </div>
          )}

          {product.description && <p style={{ fontSize: 13.5, marginTop: 14, lineHeight: 1.6 }}>{product.description}</p>}

          {error && <div className="banner banner-error" style={{ marginTop: 14 }}>{error}</div>}
          {conflict && (
            <div className="banner banner-error" style={{ marginTop: 14 }}>
              Your cart has items from another store — a Vicinia order comes from one merchant at a time.{' '}
              <button className="btn-ghost" style={{ padding: 0, display: 'inline', fontWeight: 700 }} onClick={handleSwitchStore}>
                Clear cart &amp; add this instead
              </button>
            </div>
          )}

          <div className="section-title" style={{ marginTop: 22 }}><span>Available from</span></div>
          {listings.length === 0 ? (
            <p style={{ fontSize: 13, color: 'var(--muted)' }}>No stores currently have this in stock.</p>
          ) : (
            <div className="listing-list">
              {listings.map((l) => {
                const cartQuantity = cart?.items?.find((i) => i.listingId === l.id)?.quantity || 0;
                const storeName = directory.get(l.merchantId)?.storeName;
                const highlighted = l.id === highlightedListingId;
                return (
                  <div className={`listing-row ${highlighted ? 'highlighted' : ''}`} key={l.id}>
                    <div className="merchant-tile-icon"><StoreIcon /></div>
                    <div className="listing-info">
                      {storeName && <div className="listing-store">{storeName}</div>}
                      <div className="listing-price-row">
                        <span className="price">{formatMoney(l.price)}</span>
                        <span className="stock muted">{l.availableStock > 0 ? `${l.availableStock} in stock` : 'Out of stock'}</span>
                      </div>
                    </div>
                    {cartQuantity > 0 ? (
                      <QtyStepper
                        quantity={cartQuantity}
                        busy={busyListingId === l.id}
                        maxReached={cartQuantity >= l.availableStock}
                        onIncrement={() => handleQty(l, cartQuantity + 1)}
                        onDecrement={() => handleQty(l, cartQuantity - 1)}
                      />
                    ) : (
                      <button
                        className="btn btn-sm btn-primary"
                        disabled={l.availableStock === 0 || addingId === l.id}
                        onClick={() => handleAdd(l)}
                      >
                        {addingId === l.id ? <span className="spinner" /> : 'Add to cart'}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <div className="section-title"><span>Reviews</span></div>
      {reviews.length === 0 ? (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>No reviews yet.</p>
      ) : (
        <div className="review-list">
          {reviews.map((r) => (
            <div className="review-card" key={r.id}>
              <div className="review-head">
                <div className="stars">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <StarIcon key={i} style={{ width: 13, height: 13, color: i < r.rating ? 'var(--warn)' : 'var(--line)' }} />
                  ))}
                </div>
                <span className="muted">{formatDate(r.createdAt)}</span>
              </div>
              {r.comment && <p>{r.comment}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
