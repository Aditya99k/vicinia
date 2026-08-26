import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getProfile, listAddresses } from '../api/user';
import { getCategories } from '../api/catalog';
import { nearby } from '../api/merchant';
import { CheckCircleIcon, ChevronRightIcon, ClockIcon, MapPinIcon, NavigationIcon, PackageIcon } from '../components/Icons';
import { DeliveryIllustration, GroceryBagIllustration, StorefrontIllustration } from '../components/Illustrations';
import CategoryGlyphFor from '../components/CategoryGlyphFor';
import { loadShops } from '../utils/loadShops';
import { estimateDelivery } from '../utils/deliveryEstimate';

export default function HomePage() {
  const { auth } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [profilePending, setProfilePending] = useState(false);
  const [addresses, setAddresses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [merchants, setMerchants] = useState([]);
  const [shops, setShops] = useState([]);
  const [shopsLoading, setShopsLoading] = useState(false);
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
    const address = addresses.find((a) => a.isDefault) || addresses[0];
    if (!address) return;
    // Coordinates, when on file, take over entirely server-side (real
    // distance + each merchant's own delivery radius) — city is only ever
    // the fallback for an address saved before location was required. See
    // MerchantService.nearby's own comment for why this also happens to
    // fix "Bangalore" vs "Bengaluru" style city-string mismatches.
    nearby(address.city, address.latitude, address.longitude).then(setMerchants).catch(() => setMerchants([]));
  }, [addresses]);

  useEffect(() => {
    // /api/merchants/nearby only ever returns LIVE merchants server-side
    // (MerchantSummaryResponse doesn't even carry a status field) — no
    // client-side status filter needed or possible here.
    if (merchants.length === 0) return;
    setShopsLoading(true);
    loadShops(merchants)
      .then(setShops)
      .catch(() => setShops([]))
      .finally(() => setShopsLoading(false));
  }, [merchants]);

  const defaultAddress = addresses.find((a) => a.isDefault) || addresses[0];
  const displayName = profile?.fullName || auth?.email?.split('@')[0] || 'there';

  return (
    <div className="home-grid">
      <div>
        <div className="hero-carousel">
          <div className="hero-carousel-track">
            <div className="hero-card hero-slide">
              <div className="copy">
                <span className="eyebrow">Good to see you</span>
                <h1>Hi {displayName} 👋</h1>
                <p style={{ marginTop: 8, fontSize: 13.5, color: 'var(--muted)', maxWidth: '42ch' }}>
                  Fresh groceries and everyday essentials from local stores — search for what you need to get started.
                </p>
              </div>
              <GroceryBagIllustration />
            </div>
            <div className="hero-card hero-slide">
              <div className="copy">
                <span className="eyebrow">Quick delivery</span>
                <h1>At your door in minutes</h1>
                <p style={{ marginTop: 8, fontSize: 13.5, color: 'var(--muted)', maxWidth: '42ch' }}>
                  Riders pick up straight from your nearest store — no long waits, no crowded aisles.
                </p>
              </div>
              <DeliveryIllustration />
            </div>
            <div className="hero-card hero-slide">
              <div className="copy">
                <span className="eyebrow">Shop local</span>
                <h1>Support stores near you</h1>
                <p style={{ marginTop: 8, fontSize: 13.5, color: 'var(--muted)', maxWidth: '42ch' }}>
                  Every order goes straight to a real neighbourhood shop, not a warehouse across town.
                </p>
              </div>
              <StorefrontIllustration />
            </div>
          </div>
          <div className="hero-carousel-dots">
            <span /><span /><span />
          </div>
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
            <div className="section-title"><span>Shops near you</span></div>
            {shopsLoading ? (
              <div className="page-loading"><span className="spinner" /> Loading…</div>
            ) : shops.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--muted)' }}>No stores near you have items listed yet.</p>
            ) : (
              <div className="shop-grid">
                {shops.map(({ merchant, itemCount }) => {
                  const { distanceLabel, etaLabel } = estimateDelivery(merchant);
                  return (
                    <Link to={`/store/${merchant.ownerUserId}`} className="shop-card" key={merchant.ownerUserId}>
                      <div className="shop-card-banner">
                        <StorefrontIllustration />
                        <div className="shop-card-eta"><ClockIcon /> {etaLabel}</div>
                      </div>
                      <div className="shop-card-body">
                        <div className="name">{merchant.storeName}</div>
                        <div className="shop-card-meta">
                          <span className="distance"><NavigationIcon /> {distanceLabel}</span>
                          <span className="dot" />
                          <span className="distance"><PackageIcon /> {itemCount} item{itemCount === 1 ? '' : 's'}</span>
                          <span className="dot" />
                          <span>{merchant.city}</span>
                        </div>
                      </div>
                    </Link>
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
