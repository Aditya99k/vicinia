import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getProfile, listAddresses } from '../api/user';
import { CheckCircleIcon, MapPinIcon } from '../components/Icons';
import {
  BakeryGlyph,
  CareGlyph,
  DairyGlyph,
  FruitGlyph,
  GroceryBagIllustration,
  SnackGlyph,
  VeggieGlyph,
} from '../components/Illustrations';

const CATEGORIES = [
  { label: 'Fruits', Glyph: FruitGlyph },
  { label: 'Vegetables', Glyph: VeggieGlyph },
  { label: 'Dairy', Glyph: DairyGlyph },
  { label: 'Bakery', Glyph: BakeryGlyph },
  { label: 'Snacks', Glyph: SnackGlyph },
  { label: 'Personal care', Glyph: CareGlyph },
];

export default function HomePage() {
  const { auth } = useAuth();
  const [profile, setProfile] = useState(null);
  const [profilePending, setProfilePending] = useState(false);
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setProfilePending(false);
    try {
      const [profileData, addressList] = await Promise.allSettled([getProfile(), listAddresses()]);

      if (profileData.status === 'fulfilled') {
        setProfile(profileData.value);
      } else if (profileData.reason?.response?.status === 404) {
        setProfilePending(true);
      }

      if (addressList.status === 'fulfilled') {
        setAddresses(addressList.value);
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const defaultAddress = addresses.find((a) => a.isDefault) || addresses[0];
  const displayName = profile?.fullName || auth?.email?.split('@')[0] || 'there';

  return (
    <div className="home-grid">
      <div>
        <div className="hero-card">
          <div className="copy">
            <span className="eyebrow">Good to see you</span>
            <h1>Hi {displayName} 👋</h1>
          </div>
          <GroceryBagIllustration />
        </div>

        {profilePending && (
          <div className="banner banner-success">
            <CheckCircleIcon style={{ width: 16, height: 16, flexShrink: 0, marginTop: 1 }} />
            <span>
              Setting up your profile — created by user-service reacting to your signup event on Kafka, usually a
              second or two.{' '}
              <button className="btn-ghost" style={{ padding: 0, display: 'inline', fontWeight: 700 }} onClick={load}>
                Refresh
              </button>
            </span>
          </div>
        )}

        <div className="section-title"><span>Shop by category</span></div>
        <div className="category-row">
          {CATEGORIES.map(({ label, Glyph }) => (
            <div className="category-pill" key={label} title="Catalog arrives in Stage 4">
              <div className="glyph"><Glyph /></div>
              <span>{label}</span>
            </div>
          ))}
        </div>

        <div className="coming-soon-card">
          <span className="tag">Stage 3+</span>
          <h4>Merchant &amp; catalog browsing is next</h4>
          <p>Once merchant-service and catalog-service ship, real stores and products will show up here — this screen proves auth + profile + addresses work end to end for now.</p>
        </div>
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
                  So merchants near you can find you once catalog is live.
                </div>
              </div>
            </Link>
          )}
        </div>

        <div className="sidebar-card card">
          <h4>Your account</h4>
          <div className="chip-row">
            {(auth?.roles || []).map((r) => <span key={r} className="badge">{r}</span>)}
          </div>
          <div style={{ height: 8 }} />
          <div className="chip-row">
            {(auth?.permissions || []).map((p) => <span key={p} className="badge badge-muted">{p}</span>)}
          </div>
        </div>
      </div>
    </div>
  );
}
