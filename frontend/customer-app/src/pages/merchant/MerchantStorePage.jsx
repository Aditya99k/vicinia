import { useEffect, useState } from 'react';
import { getMe, updateHours, updateMe } from '../../api/merchant';
import { NavigationIcon } from '../../components/Icons';

export default function MerchantStorePage() {
  const [form, setForm] = useState(null);
  const [hours, setHours] = useState({ openTime: '09:00', closeTime: '21:00' });
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingHours, setSavingHours] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');

  useEffect(() => {
    getMe()
      .then((m) => {
        setForm({
          storeName: m.storeName || '', description: m.description || '', addressLine1: m.addressLine1 || '',
          city: m.city || '', state: m.state || '', pincode: m.pincode || '',
          latitude: m.latitude ?? '', longitude: m.longitude ?? '', deliveryRadiusKm: m.deliveryRadiusKm ?? 5,
        });
        if (m.openTime) setHours({ openTime: m.openTime.slice(0, 5), closeTime: m.closeTime?.slice(0, 5) || '21:00' });
      })
      .catch(() => setError('Could not load your store profile.'))
      .finally(() => setLoading(false));
  }, []);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function useMyLocation() {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition((pos) => {
      set('latitude', pos.coords.latitude);
      set('longitude', pos.coords.longitude);
    });
  }

  async function handleSaveProfile(e) {
    e.preventDefault();
    setSavingProfile(true);
    setError('');
    try {
      await updateMe({
        ...form,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
        deliveryRadiusKm: Number(form.deliveryRadiusKm),
      });
      setSaved('profile');
      setTimeout(() => setSaved(''), 2500);
    } catch {
      setError('Could not save your store profile.');
    } finally {
      setSavingProfile(false);
    }
  }

  async function handleSaveHours(e) {
    e.preventDefault();
    setSavingHours(true);
    setError('');
    try {
      await updateHours({ openTime: `${hours.openTime}:00`, closeTime: `${hours.closeTime}:00` });
      setSaved('hours');
      setTimeout(() => setSaved(''), 2500);
    } catch {
      setError('Could not save your hours.');
    } finally {
      setSavingHours(false);
    }
  }

  if (loading || !form) return <div className="page-loading"><span className="spinner" /> Loading…</div>;

  return (
    <div className="profile-page">
      <h1 style={{ fontSize: 22, marginBottom: 18 }}>Store settings</h1>
      {error && <div className="banner banner-error">{error}</div>}

      <div className="section-title"><span>Store profile</span></div>
      <form onSubmit={handleSaveProfile} className="card" style={{ marginBottom: 20 }}>
        {saved === 'profile' && <div className="banner banner-success">Saved.</div>}
        <div className="field">
          <label htmlFor="storeName">Store name</label>
          <input id="storeName" value={form.storeName} onChange={(e) => set('storeName', e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea id="description" rows={2} value={form.description} onChange={(e) => set('description', e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="addressLine1">Address</label>
          <input id="addressLine1" value={form.addressLine1} onChange={(e) => set('addressLine1', e.target.value)} required />
        </div>
        <div className="field-row">
          <div className="field">
            <label htmlFor="city">City</label>
            <input id="city" value={form.city} onChange={(e) => set('city', e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="pincode">Pincode</label>
            <input id="pincode" value={form.pincode} onChange={(e) => set('pincode', e.target.value)} required />
          </div>
        </div>

        <div className="field">
          <label>Location</label>
          <button type="button" className="btn btn-secondary btn-sm" onClick={useMyLocation} style={{ marginBottom: 8, alignSelf: 'flex-start' }}>
            <NavigationIcon style={{ width: 14, height: 14 }} /> Use my current location
          </button>
        </div>
        <div className="field-row">
          <div className="field">
            <label htmlFor="latitude">Latitude</label>
            <input id="latitude" type="number" step="any" value={form.latitude} onChange={(e) => set('latitude', e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="longitude">Longitude</label>
            <input id="longitude" type="number" step="any" value={form.longitude} onChange={(e) => set('longitude', e.target.value)} required />
          </div>
        </div>
        <div className="field">
          <label htmlFor="radius">Delivery radius (km)</label>
          <input id="radius" type="number" step="0.5" min="0.5" value={form.deliveryRadiusKm} onChange={(e) => set('deliveryRadiusKm', e.target.value)} required />
        </div>

        <button className="btn btn-primary btn-block" disabled={savingProfile} style={{ marginTop: 4 }}>
          {savingProfile ? <span className="spinner" /> : 'Save store profile'}
        </button>
      </form>

      <div className="section-title"><span>Operating hours</span></div>
      <form onSubmit={handleSaveHours} className="card">
        {saved === 'hours' && <div className="banner banner-success">Saved.</div>}
        <div className="field-row">
          <div className="field">
            <label htmlFor="openTime">Opens</label>
            <input id="openTime" type="time" value={hours.openTime} onChange={(e) => setHours((h) => ({ ...h, openTime: e.target.value }))} required />
          </div>
          <div className="field">
            <label htmlFor="closeTime">Closes</label>
            <input id="closeTime" type="time" value={hours.closeTime} onChange={(e) => setHours((h) => ({ ...h, closeTime: e.target.value }))} required />
          </div>
        </div>
        <button className="btn btn-primary btn-block" disabled={savingHours}>
          {savingHours ? <span className="spinner" /> : 'Save hours'}
        </button>
      </form>
    </div>
  );
}
