import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useProductImages } from '../hooks/useProductImages';
import { CheckCircleIcon } from './Icons';
import ProductImage from './ProductImage';
import { formatMoney } from '../utils/format';

const VISIBLE_MS = 4000;

/** Slides in from the right the moment something lands in the cart, with a direct path to checkout — so adding an item doesn't mean hunting for the cart icon to actually buy it. */
export default function CartAddedToast() {
  const { lastAdded, itemCount, cart } = useCart();
  const navigate = useNavigate();
  const [visible, setVisible] = useState(false);
  const { imageFor, categoryFor } = useProductImages(lastAdded ? [lastAdded.item.productId] : []);

  useEffect(() => {
    if (!lastAdded) return;
    setVisible(true);
    const timer = setTimeout(() => setVisible(false), VISIBLE_MS);
    return () => clearTimeout(timer);
  }, [lastAdded]);

  if (!lastAdded || !visible) return null;
  const { item } = lastAdded;

  return (
    <div className="cart-added-toast">
      <div className="cart-added-toast-head">
        <CheckCircleIcon style={{ width: 15, height: 15, color: 'var(--success)' }} />
        <span>Added to cart</span>
        <button className="cart-added-toast-close" onClick={() => setVisible(false)} aria-label="Dismiss">✕</button>
      </div>
      <div className="cart-added-toast-item">
        <div className="cart-added-toast-thumb">
          <ProductImage src={imageFor(item.productId)} name={item.productName} category={categoryFor(item.productId)} />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="name">{item.productName}</div>
          <div className="muted">{formatMoney(item.price)} × {item.quantity}</div>
        </div>
      </div>
      <div className="cart-added-toast-summary muted">
        {itemCount} item{itemCount === 1 ? '' : 's'} in cart · {formatMoney(cart?.subtotal)}
      </div>
      <div className="cart-added-toast-actions">
        <button className="btn btn-secondary btn-sm" onClick={() => { setVisible(false); navigate('/cart'); }}>View cart</button>
        <button className="btn btn-primary btn-sm" onClick={() => { setVisible(false); navigate('/checkout'); }}>Checkout</button>
      </div>
    </div>
  );
}
