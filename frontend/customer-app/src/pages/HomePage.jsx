import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getProfile, listAddresses } from '../api/user';
import { CheckCircleIcon, MapPinIcon, StoreIcon } from '../components/Icons';

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
        // Created asynchronously by user-service consuming user.registered —
        // can take a moment right after signup.
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
    <div>
      <div className="hero-greeting">
        <span className="eyebrow">Good to see you</span>
        <h1>Hi {displayName} 👋</h1>
      </div>

      {profilePending && (
        <div className="banner banner-success">
          <CheckCircleIcon style={{ width: 16, height: 16, flexShrink: 0, marginTop: 1 }} />
          <span>
            Setting up your profile — this lands within a second or two, created by user-service reacting to your
            signup event on Kafka.{' '}
            <button className="btn-ghost" style={{ padding: 0, display: 'inline', fontWeight: 700 }} onClick={load}>
              Refresh
            </button>
          </span>
        </div>
      )}

      <div className="stat-grid">
        <div className="stat-card">
          <div className="num">{auth?.roles?.length ?? 0}</div>
          <div className="label">Role{auth?.roles?.length === 1 ? '' : 's'}</div>
        </div>
        <div className="stat-card">
          <div className="num">{auth?.permissions?.length ?? 0}</div>
          <div className="label">Permissions</div>
        </div>
      </div>

      <div className="section-title">
        <span>Delivery address</span>
        <Link to="/addresses">{addresses.length ? 'Manage' : 'Add one'}</Link>
      </div>

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : defaultAddress ? (
        <div className="card default-address-card">
          <div className="pin"><MapPinIcon /></div>
          <div>
            <div style={{ fontWeight: 700, fontSize: 14.5 }}>{defaultAddress.label}</div>
            <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 2 }}>
              {defaultAddress.line1}, {defaultAddress.city} — {defaultAddress.pincode}
            </div>
          </div>
        </div>
      ) : (
        <Link to="/addresses" className="card default-address-card" style={{ display: 'flex' }}>
          <div className="pin"><MapPinIcon /></div>
          <div>
            <div style={{ fontWeight: 700, fontSize: 14.5 }}>Add a delivery address</div>
            <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 2 }}>
              So merchants near you can find you once catalog is live.
            </div>
          </div>
        </Link>
      )}

      <div className="section-title">
        <span>Stores near you</span>
      </div>
      <div className="coming-soon-card">
        <span className="tag">Stage 3+</span>
        <StoreIcon style={{ width: 32, height: 32, margin: '4px auto 8px', display: 'block' }} />
        <h4>Merchant &amp; catalog browsing is next</h4>
        <p>Once merchant-service and catalog-service ship, real stores will show up here — this screen is just proving auth + profile + addresses work end to end for now.</p>
      </div>
    </div>
  );
}
