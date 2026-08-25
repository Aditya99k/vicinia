import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getProfile, listAddresses } from '../api/user';
import { getCategories } from '../api/catalog';
import { nearby } from '../api/merchant';
import { CheckCircleIcon, ChevronRightIcon, MapPinIcon, StoreIcon } from '../components/Icons';
import { GroceryBagIllustration } from '../components/Illustrations';
import CategoryGlyphFor from '../components/CategoryGlyphFor';
import ProductImage from '../components/ProductImage';
import { loadStorefronts } from '../utils/loadStorefronts';
import { formatMoney } from '../utils/format';

export default function HomePage() {
  const { auth } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [profilePending, setProfilePending] = useState(false);
  const [addresses, setAddresses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [merchants, setMerchants] = useState([]);
  const [storefronts, setStorefronts] = useState({});
  const [storefrontsLoading, setStorefrontsLoading] = useState(false);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setProfilePending(false);
    try {
      const [profileData, addressList, cats] = await Promise.allSettled([
        getProfile(),
        listAddresses(),
        getCategories(),
      ]);

      if (profileData.status === 'fulfilled') {
        setProfile(profileData.value);
      } else if (profileData.reason?.response?.status === 404) {
        setProfilePending(true);
      }
      if (addressList.status === 'fulfilled') setAddresses(addressList.value);
      if (cats.status === 'fulfilled') setCategories(cats.value);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const city = addresses.find((a) => a.isDefault)?.city || addresses[0]?.city;
    if (!city) return;
    nearby(city).then(setMerchants).catch(() => setMerchants([]));
  }, [addresses]);

  useEffect(() => {
    // /api/merchants/nearby only ever returns LIVE merchants server-side
    // (MerchantSummaryResponse doesn't even carry a status field) — no
    // client-side status filter needed or possible here.
    if (merchants.length === 0) return;
    setStorefrontsLoading(true);
    loadStorefronts(merchants)
      .then(setStorefronts)
      .catch(() => setStorefronts({}))
      .finally(() => setStorefrontsLoading(false));
  }, [merchants]);

  const defaultAddress = addresses.find((a) => a.isDefault) || addresses[0];
  const displayName = profile?.fullName || auth?.email?.split('@')[0] || 'there';

  return (
    <div className="home-grid">
      <div>
        <div className="hero-card">
          <div className="copy">
            <span className="eyebrow">Good to see you</span>
            <h1>Hi {displayName} 👋</h1>
            <p style={{ marginTop: 8, fontSize: 13.5, color: 'var(--muted)', maxWidth: '42ch' }}>
              Fresh groceries and everyday essentials from local stores — search for what you need to get started.
            </p>
          </div>
          <GroceryBagIllustration />
        </div>

        {profilePending && (
          <div className="banner banner-success">
            <CheckCircleIcon style={{ width: 16, height: 16, flexShrink: 0, marginTop: 1 }} />
            <span>
              Setting up your profile — usually just a second or two.{' '}
              <button className="btn-ghost" style={{ padding: 0, display: 'inline', fontWeight: 700 }} onClick={load}>
                Refresh
              </button>
            </span>
          </div>
        )}

        <div className="section-title"><span>Shop by category</span></div>
        {loading ? (
          <div className="page-loading"><span className="spinner" /> Loading…</div>
        ) : (
          <div className="category-row">
            {categories.map((c) => (
              <Link className="category-pill" key={c.id} to={`/search?category=${encodeURIComponent(c.name)}`}>
                <div className="glyph"><CategoryGlyphFor name={c.name} /></div>
                <span>{c.name}</span>
              </Link>
            ))}
          </div>
        )}

        {merchants.length > 0 && (
          <>
            <div className="section-title"><span>Stores near you</span></div>
            {storefrontsLoading ? (
              <div className="page-loading"><span className="spinner" /> Loading…</div>
            ) : (
              <div className="storefront-grid">
                {merchants
                  .filter((m) => (storefronts[m.ownerUserId] || []).length > 0)
                  .map((m) => {
                    const items = storefronts[m.ownerUserId] || [];
                    return (
                      <div className="storefront" key={m.id}>
                        <div className="storefront-head">
                          <div className="merchant-tile-icon"><StoreIcon /></div>
                          <div>
                            <div className="name">{m.storeName}</div>
                            <div className="meta">{m.city}</div>
                          </div>
                        </div>
                        <div className="storefront-items">
                          {items.map((item) => (
                            <Link to={`/product/${item.productId}?listing=${item.id}`} className="storefront-item" key={item.id} title={item.productName}>
                              <div className="storefront-item-image">
                                <ProductImage src={item.productImage} name={item.productName} />
                              </div>
                              <div className="storefront-item-price">{formatMoney(item.price)}</div>
                            </Link>
                          ))}
                        </div>
                      </div>
                    );
                  })}
              </div>
            )}
          </>
        )}
      </div>

      <div>
        <div className="sidebar-card">
          <div className="section-title" style={{ margin: '0 0 10px' }}>
            <span>Delivery address</span>
            <Link to="/addresses">{addresses.length ? 'Manage' : 'Add one'}</Link>
          </div>
          {loading ? (
            <div className="page-loading"><span className="spinner" /> Loading…</div>
          ) : defaultAddress ? (
            <div className="card default-address-card">
              <div className="pin"><MapPinIcon /></div>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{defaultAddress.label}</div>
                <div style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>
                  {defaultAddress.line1}, {defaultAddress.city} — {defaultAddress.pincode}
                </div>
              </div>
            </div>
          ) : (
            <Link to="/addresses" className="card default-address-card" style={{ display: 'flex' }}>
              <div className="pin"><MapPinIcon /></div>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>Add a delivery address</div>
                <div style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>
                  So merchants near you can find you.
                </div>
              </div>
            </Link>
          )}
        </div>

        <div className="sidebar-card card quick-link" onClick={() => navigate('/orders')}>
          <div>
            <h4 style={{ marginBottom: 2 }}>Your orders</h4>
            <p style={{ fontSize: 12.5, color: 'var(--muted)' }}>Track deliveries, reorder favorites</p>
          </div>
          <ChevronRightIcon style={{ width: 18, height: 18, color: 'var(--faint)' }} />
        </div>

        <div className="sidebar-card card quick-link" onClick={() => navigate('/wallet')}>
          <div>
            <h4 style={{ marginBottom: 2 }}>Wallet</h4>
            <p style={{ fontSize: 12.5, color: 'var(--muted)' }}>Top up for faster checkout</p>
          </div>
          <ChevronRightIcon style={{ width: 18, height: 18, color: 'var(--faint)' }} />
        </div>
      </div>
    </div>
  );
}
