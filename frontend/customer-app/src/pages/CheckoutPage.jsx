import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { listAddresses } from '../api/user';
import { getWalletBalance, verifyRazorpayPayment } from '../api/payment';
import { getOrder, placeOrder } from '../api/order';
import { BanknoteIcon, CreditCardIcon, MapPinIcon, WalletIcon } from '../components/Icons';
import ShopBanner from '../components/ShopBanner';
import { formatMoney } from '../utils/format';

function loadRazorpayScript() {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = resolve;
    script.onerror = () => reject(new Error('Could not load Razorpay checkout.'));
    document.body.appendChild(script);
  });
}

export default function CheckoutPage() {
  const { cart, refresh } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const couponCode = location.state?.couponCode || null;

  const [addresses, setAddresses] = useState([]);
  const [balance, setBalance] = useState(null);
  const [method, setMethod] = useState('WALLET');
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState('');
  const [waitingForPayment, setWaitingForPayment] = useState(false);

  useEffect(() => {
    listAddresses().then(setAddresses).catch(() => setAddresses([]));
    getWalletBalance().then(setBalance).catch(() => setBalance(null));
  }, []);

  const items = cart?.items || [];
  const subtotal = cart?.subtotal || 0;
  const defaultAddress = addresses.find((a) => a.isDefault) || addresses[0];
  const insufficientWallet = balance != null && method === 'WALLET' && Number(balance.balance) < subtotal;

  async function pollUntilResolved(orderId, attempts = 6) {
    for (let i = 0; i < attempts; i++) {
      const order = await getOrder(orderId);
      if (order.status !== 'PAYMENT_PENDING' && order.status !== 'CREATED') {
        return order;
      }
      await new Promise((r) => setTimeout(r, 1000));
    }
    return getOrder(orderId);
  }

  async function handlePlaceOrder() {
    setError('');
    setPlacing(true);
    try {
      const order = await placeOrder({
        couponCode, paymentMethod: method,
        deliveryLatitude: defaultAddress?.latitude, deliveryLongitude: defaultAddress?.longitude,
        deliveryAddressLine: defaultAddress
          ? `${defaultAddress.label} — ${defaultAddress.line1}${defaultAddress.line2 ? ', ' + defaultAddress.line2 : ''}, ${defaultAddress.city} ${defaultAddress.pincode}`
          : null,
      });

      if (method === 'RAZORPAY' && order.razorpayOrderId) {
        await loadRazorpayScript();
        const rzp = new window.Razorpay({
          key: order.razorpayKeyId,
          order_id: order.razorpayOrderId,
          name: 'Vicinia',
          description: `Order ${order.id}`,
          theme: { color: '#F8C200' },
          handler: async (response) => {
            setWaitingForPayment(true);
            try {
              // The webhook this otherwise relies on can only ever reach a
              // publicly-hosted deployment — Razorpay's servers can't call
              // back into a local dev stack — so this verifies the same
              // signature server-side right from the browser's own success
              // callback instead of waiting on an inbound call that will
              // never arrive here.
              await verifyRazorpayPayment({
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              });
            } catch {
              // Falls through to polling below regardless — a real webhook
              // racing this (or arriving instead of it) still resolves the
              // order; verify failing here doesn't strand the customer.
            }
            const resolved = await pollUntilResolved(order.id);
            await refresh();
            navigate(`/orders/${resolved.id}`);
          },
          modal: {
            ondismiss: () => setPlacing(false),
          },
        });
        rzp.open();
        return;
      }

      await refresh();
      navigate(`/orders/${order.id}`);
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not place your order.');
      setPlacing(false);
    }
  }

  if (!cart || items.length === 0) {
    return <div className="empty-state"><h3>Your cart is empty</h3><p>Add items to your cart before checking out.</p></div>;
  }

  return (
    <div className="checkout-page">
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Checkout</h1>
      <ShopBanner merchantId={cart?.merchantId} />

      <div className="cart-layout">
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="section-title" style={{ margin: '0 0 10px' }}>
              <span>Delivery address</span>
            </div>
            {defaultAddress ? (
              <div className="default-address-card">
                <div className="pin"><MapPinIcon /></div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: 14 }}>{defaultAddress.label}</div>
                  <div style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>
                    {defaultAddress.line1}, {defaultAddress.city} — {defaultAddress.pincode}
                  </div>
                </div>
              </div>
            ) : (
              <p style={{ fontSize: 13, color: 'var(--danger)' }}>Add a delivery address before placing an order.</p>
            )}
          </div>

          <div className="card">
            <div className="section-title" style={{ margin: '0 0 10px' }}><span>Payment method</span></div>
            <div className="payment-options">
              <label className={`payment-option ${method === 'WALLET' ? 'selected' : ''}`}>
                <input type="radio" name="method" checked={method === 'WALLET'} onChange={() => setMethod('WALLET')} />
                <WalletIcon style={{ width: 18, height: 18 }} />
                <div>
                  <div className="title">Wallet</div>
                  <div className="muted">{balance ? `Balance: ${formatMoney(balance.balance)}` : 'Loading balance…'}</div>
                </div>
              </label>
              <label className={`payment-option ${method === 'RAZORPAY' ? 'selected' : ''}`}>
                <input type="radio" name="method" checked={method === 'RAZORPAY'} onChange={() => setMethod('RAZORPAY')} />
                <CreditCardIcon style={{ width: 18, height: 18 }} />
                <div>
                  <div className="title">Card / UPI (Razorpay)</div>
                  <div className="muted">Test mode — no real money moves</div>
                </div>
              </label>
              <label className={`payment-option ${method === 'COD' ? 'selected' : ''}`}>
                <input type="radio" name="method" checked={method === 'COD'} onChange={() => setMethod('COD')} />
                <BanknoteIcon style={{ width: 18, height: 18 }} />
                <div>
                  <div className="title">Cash on Delivery</div>
                  <div className="muted">Pay the delivery partner in cash or UPI on arrival</div>
                </div>
              </label>
            </div>
            {insufficientWallet && (
              <div className="banner banner-error" style={{ marginTop: 12, marginBottom: 0 }}>
                Insufficient wallet balance for this order — top up your wallet or pay with Razorpay instead.
              </div>
            )}
          </div>
        </div>

        <div className="card summary-card">
          <div className="section-title" style={{ margin: '0 0 10px' }}><span>Order summary</span></div>
          {items.map((i) => (
            <div className="summary-row" key={i.listingId}>
              <span>{i.productName} × {i.quantity}</span>
              <span>{formatMoney(i.lineTotal)}</span>
            </div>
          ))}
          <div className="summary-row total"><span>Total</span><span>{formatMoney(subtotal)}</span></div>

          {error && <div className="banner banner-error" style={{ marginTop: 12 }}>{error}</div>}

          <button
            className="btn btn-primary btn-block"
            style={{ marginTop: 14 }}
            disabled={placing || !defaultAddress || insufficientWallet}
            onClick={handlePlaceOrder}
          >
            {placing ? <span className="spinner" /> : waitingForPayment ? 'Confirming payment…' : `Place order — ${formatMoney(subtotal)}`}
          </button>
        </div>
      </div>
    </div>
  );
}
