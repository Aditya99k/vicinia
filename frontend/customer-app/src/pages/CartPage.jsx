import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { validateCoupon } from '../api/coupon';
import { MinusIcon, PlusIcon, TrashIcon } from '../components/Icons';
import { EmptyBoxIllustration } from '../components/Illustrations';
import ShopBanner from '../components/ShopBanner';
import ProductImage from '../components/ProductImage';
import { useProductImages } from '../hooks/useProductImages';
import { useActionDialog } from '../hooks/useActionDialog';
import { formatMoney } from '../utils/format';

export default function CartPage() {
  const { cart, loading, updateItem, removeItem, clear } = useCart();
  const navigate = useNavigate();
  const { confirm, dialog } = useActionDialog();

  const [couponCode, setCouponCode] = useState('');
  const [couponResult, setCouponResult] = useState(null);
  const [couponError, setCouponError] = useState('');
  const [checking, setChecking] = useState(false);
  const [busyListingId, setBusyListingId] = useState(null);

  const items = cart?.items || [];
  const subtotal = cart?.subtotal || 0;
  const imageFor = useProductImages(items.map((i) => i.productId));

  async function handleClearCart() {
    if (!(await confirm('Remove all items from your cart?', { title: 'Clear cart', danger: true, confirmLabel: 'Clear cart' }))) return;
    await clear();
  }

  async function handleQty(listingId, quantity) {
    setBusyListingId(listingId);
    try {
      if (quantity <= 0) {
        await removeItem(listingId);
      } else {
        await updateItem(listingId, quantity);
      }
    } finally {
      setBusyListingId(null);
    }
  }

  async function handleCheckCoupon(e) {
    e.preventDefault();
    if (!couponCode.trim()) return;
    setChecking(true);
    setCouponError('');
    setCouponResult(null);
    try {
      const result = await validateCoupon(couponCode.trim(), subtotal);
      setCouponResult(result);
    } catch (err) {
      setCouponError(err?.response?.data?.error || 'That coupon code isn’t valid for this order.');
    } finally {
      setChecking(false);
    }
  }

  function goToCheckout() {
    navigate('/checkout', { state: { couponCode: couponResult ? couponCode.trim() : null } });
  }

  if (loading && !cart) return <div className="page-loading"><span className="spinner" /> Loading…</div>;

  if (items.length === 0) {
    return (
      <div className="empty-state">
        {dialog}
        <EmptyBoxIllustration />
        <h3>Your cart is empty</h3>
        <p>Search for products and add them to your cart to see them here.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: 8 }}>Start shopping</Link>
      </div>
    );
  }

  return (
    <div className="cart-page">
      {dialog}
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Your cart</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 14 }}>
        {items.length} item{items.length === 1 ? '' : 's'} from one store
      </p>
      <ShopBanner merchantId={cart?.merchantId} />

      <div className="cart-layout">
        <div className="cart-items card">
          {items.map((item) => (
            <div className="cart-item" key={item.listingId}>
              <div className="cart-item-thumb">
                <ProductImage src={imageFor(item.productId)} name={item.productName} />
              </div>
              <div className="cart-item-body">
                <div className="name">{item.productName}</div>
                {item.available ? (
                  <div className="muted">{formatMoney(item.price)} each</div>
                ) : (
                  <div className="unavailable">No longer available</div>
                )}
              </div>
              {item.available && (
                <div className="qty-stepper">
                  <button onClick={() => handleQty(item.listingId, item.quantity - 1)} disabled={busyListingId === item.listingId}>
                    <MinusIcon style={{ width: 13, height: 13 }} />
                  </button>
                  <span>{busyListingId === item.listingId ? <span className="spinner" /> : item.quantity}</span>
                  <button
                    onClick={() => handleQty(item.listingId, item.quantity + 1)}
                    disabled={busyListingId === item.listingId || item.quantity >= item.availableStock}
                  >
                    <PlusIcon style={{ width: 13, height: 13 }} />
                  </button>
                </div>
              )}
              <div className="cart-item-total">{item.available ? formatMoney(item.lineTotal) : ''}</div>
              <button className="icon-btn" onClick={() => handleQty(item.listingId, 0)} aria-label="Remove">
                <TrashIcon style={{ width: 15, height: 15 }} />
              </button>
            </div>
          ))}
          <button className="btn btn-outline-danger btn-sm" onClick={handleClearCart} style={{ marginTop: 10 }}>
            <TrashIcon style={{ width: 14, height: 14 }} /> Clear cart
          </button>
        </div>

        <div>
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="section-title" style={{ margin: '0 0 10px' }}><span>Have a coupon?</span></div>
            <form onSubmit={handleCheckCoupon} style={{ display: 'flex', gap: 8 }}>
              <input
                placeholder="Enter code"
                value={couponCode}
                onChange={(e) => { setCouponCode(e.target.value.toUpperCase()); setCouponResult(null); setCouponError(''); }}
                style={{ flex: 1, border: '1.5px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '9px 12px', fontSize: 13.5, background: 'var(--bg)', color: 'var(--ink)' }}
              />
              <button className="btn btn-secondary btn-sm" disabled={checking}>{checking ? <span className="spinner" /> : 'Apply'}</button>
            </form>
            {couponError && <div className="banner banner-error" style={{ marginTop: 10, marginBottom: 0 }}>{couponError}</div>}
            {couponResult && (
              <div className="banner banner-success" style={{ marginTop: 10, marginBottom: 0 }}>
                {formatMoney(couponResult.discountAmount)} off applied at checkout.
              </div>
            )}
          </div>

          <div className="card summary-card">
            <div className="summary-row"><span>Subtotal</span><span>{formatMoney(subtotal)}</span></div>
            {couponResult && (
              <div className="summary-row discount"><span>Coupon discount</span><span>−{formatMoney(couponResult.discountAmount)}</span></div>
            )}
            <div className="summary-row total">
              <span>Total</span>
              <span>{formatMoney(couponResult ? subtotal - couponResult.discountAmount : subtotal)}</span>
            </div>
            <button className="btn btn-primary btn-block" style={{ marginTop: 14 }} onClick={goToCheckout}>
              Proceed to checkout
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
