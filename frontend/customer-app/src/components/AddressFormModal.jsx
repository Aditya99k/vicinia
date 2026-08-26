import { useState } from 'react';
import { CheckCircleIcon, NavigationIcon } from './Icons';

const emptyForm = {
  label: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  pincode: '',
  isDefault: false,
  latitude: null,
  longitude: null,
};

export default function AddressFormModal({ initial, onClose, onSubmit, submitting, error }) {
  const [form, setForm] = useState(() => (initial ? { ...emptyForm, ...initial } : emptyForm));
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState('');

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function useCurrentLocation() {
    if (!navigator.geolocation) {
      setLocationError('Your browser does not support location access.');
      return;
    }
    setLocating(true);
    setLocationError('');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        set('latitude', pos.coords.latitude);
        set('longitude', pos.coords.longitude);
        setLocating(false);
      },
      () => {
        setLocationError('Could not get your location — enable location access and try again.');
        setLocating(false);
      }
    );
  }

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit(form);
  }

  const hasLocation = form.latitude != null && form.longitude != null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-sheet" onClick={(e) => e.stopPropagation()}>
        <div className="modal-handle" />
        <div className="modal-header">
          <h3>{initial ? 'Edit address' : 'Add new address'}</h3>
          <button className="modal-close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {error && <div className="banner banner-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="label">Label</label>
            <input id="label" placeholder="Home, Work, ..." value={form.label} onChange={(e) => set('label', e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="line1">Address line 1</label>
            <input id="line1" placeholder="House / flat / street" value={form.line1} onChange={(e) => set('line1', e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="line2">Address line 2 (optional)</label>
            <input id="line2" placeholder="Landmark, area" value={form.line2} onChange={(e) => set('line2', e.target.value)} />
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="city">City</label>
              <input id="city" value={form.city} onChange={(e) => set('city', e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="state">State</label>
              <input id="state" value={form.state} onChange={(e) => set('state', e.target.value)} required />
            </div>
          </div>
          <div className="field">
            <label htmlFor="pincode">Pincode</label>
            <input id="pincode" value={form.pincode} onChange={(e) => set('pincode', e.target.value)} required />
          </div>

          <div className="field">
            <label>Precise location (required)</label>
            <button type="button" className="btn btn-secondary btn-sm" onClick={useCurrentLocation} disabled={locating} style={{ alignSelf: 'flex-start' }}>
              {locating ? (
                <span className="spinner" />
              ) : hasLocation ? (
                <><CheckCircleIcon style={{ width: 14, height: 14, color: 'var(--success)' }} /> Location captured</>
              ) : (
                <><NavigationIcon style={{ width: 14, height: 14 }} /> Use my current location</>
              )}
            </button>
            <p style={{ fontSize: 11.5, color: 'var(--muted)', marginTop: 6 }}>
              Required — this is how stores near you get found, and how your delivery partner navigates
              straight to your door instead of just a text description.
            </p>
            {locationError && <p style={{ fontSize: 12, color: 'var(--danger)', marginTop: 4 }}>{locationError}</p>}
            {!hasLocation && !locating && !locationError && (
              <p style={{ fontSize: 12, color: 'var(--muted)', marginTop: 4 }}>
                Tap "Use my current location" above to continue.
              </p>
            )}
          </div>

          <div className="checkbox-row">
            <input
              id="isDefault"
              type="checkbox"
              checked={form.isDefault}
              onChange={(e) => set('isDefault', e.target.checked)}
            />
            <label htmlFor="isDefault">Set as default delivery address</label>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={submitting || !hasLocation}>
              {submitting ? <span className="spinner" /> : initial ? 'Save changes' : 'Add address'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
