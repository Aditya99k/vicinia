import { useState } from 'react';

const emptyForm = {
  label: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  pincode: '',
  isDefault: false,
};

export default function AddressFormModal({ initial, onClose, onSubmit, submitting, error }) {
  const [form, setForm] = useState(() => (initial ? { ...emptyForm, ...initial } : emptyForm));

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit(form);
  }

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
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? <span className="spinner" /> : initial ? 'Save changes' : 'Add address'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
