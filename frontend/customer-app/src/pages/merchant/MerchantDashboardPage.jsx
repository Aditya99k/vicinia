import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { closeStore, getMe, goLive, reopenStore } from '../../api/merchant';
import { myListings } from '../../api/inventory';
import StatusBadge from '../../components/StatusBadge';
import MerchantOrderQueue from '../../components/MerchantOrderQueue';
import { ChevronRightIcon, PackageIcon, StoreIcon } from '../../components/Icons';

export default function MerchantDashboardPage() {
  const [merchant, setMerchant] = useState(null);
  const [notApplied, setNotApplied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [listingCount, setListingCount] = useState(0);

  function load() {
    setLoading(true);
    setNotApplied(false);
    getMe()
      .then((m) => {
        setMerchant(m);
        if (m.status === 'LIVE') {
          myListings().then((l) => setListingCount(l.length)).catch(() => {});
        }
      })
      .catch((err) => {
        if (err?.response?.status === 404) setNotApplied(true);
      })
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleGoLive() {
    setBusy(true);
    setError('');
    try {
      await goLive();
      load();
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not go live — make sure your store profile and hours are set.');
    } finally {
      setBusy(false);
    }
  }

  async function handleToggleOpen() {
    setBusy(true);
    setError('');
    try {
      if (merchant.status === 'TEMP_CLOSED') await reopenStore();
      else await closeStore();
      load();
    } catch {
      setError('Could not update your store status.');
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <div className="page-loading"><span className="spinner" /> Loading…</div>;
  if (notApplied) return <Navigate to="/merchant/apply" replace />;
  if (!merchant) return null;

  return (
    <div>
      <div className="addresses-header">
        <div>
          <h1>{merchant.storeName}</h1>
          <p>{merchant.city}, {merchant.state}</p>
        </div>
        <StatusBadge status={merchant.status} />
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {merchant.status === 'PENDING_REVIEW' && (
        <div className="coming-soon-card">
          <span className="tag">Under review</span>
          <h4>Your application is being reviewed</h4>
          <p>An admin will approve or reject it soon — check back here for updates.</p>
        </div>
      )}

      {merchant.status === 'REJECTED' && (
        <div className="banner banner-error">
          Your application was rejected{merchant.rejectionReason ? `: ${merchant.rejectionReason}` : '.'}
        </div>
      )}

      {(merchant.status === 'APPROVED' || merchant.status === 'ONBOARDING') && (
        <div className="coming-soon-card">
          <span className="tag">Almost there</span>
          <h4>Finish setting up your store</h4>
          <p>Add your location, delivery radius, and operating hours, then go live.</p>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 14 }}>
            <Link to="/merchant/store" className="btn btn-secondary btn-sm">Edit store details</Link>
            <button className="btn btn-primary btn-sm" onClick={handleGoLive} disabled={busy}>
              {busy ? <span className="spinner" /> : 'Go live'}
            </button>
          </div>
        </div>
      )}

      {merchant.status === 'SUSPENDED' && (
        <div className="banner banner-error">
          Your store has been suspended by an admin{merchant.suspensionReason ? `: ${merchant.suspensionReason}` : '.'}
        </div>
      )}

      {merchant.status === 'PERMANENTLY_CLOSED' && (
        <div className="banner banner-error">This store has been permanently closed.</div>
      )}

      {(merchant.status === 'LIVE' || merchant.status === 'TEMP_CLOSED') && (
        <>
          <div className="dashboard-stats">
            <Link to="/merchant/listings" className="card stat-card">
              <div className="stat-icon"><PackageIcon style={{ width: 18, height: 18 }} /></div>
              <div>
                <div className="stat-value">{listingCount}</div>
                <div className="muted">Active listings</div>
              </div>
              <ChevronRightIcon className="chev" style={{ width: 16, height: 16 }} />
            </Link>
            <Link to="/merchant/store" className="card stat-card">
              <div className="stat-icon"><StoreIcon style={{ width: 18, height: 18 }} /></div>
              <div>
                <div className="stat-value">{merchant.status === 'LIVE' ? 'Open' : 'Closed'}</div>
                <div className="muted">Store status</div>
              </div>
              <ChevronRightIcon className="chev" style={{ width: 16, height: 16 }} />
            </Link>
          </div>

          <button className="btn btn-secondary" onClick={handleToggleOpen} disabled={busy} style={{ marginTop: 18 }}>
            {busy ? <span className="spinner" /> : merchant.status === 'TEMP_CLOSED' ? 'Reopen store' : 'Temporarily close store'}
          </button>

          <MerchantOrderQueue />
        </>
      )}
    </div>
  );
}
