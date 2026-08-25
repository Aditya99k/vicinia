import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apply } from '../../api/merchant';
import { StorefrontIllustration } from '../../components/Illustrations';

export default function MerchantApplyPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    storeName: '', description: '', addressLine1: '', city: '', state: '', pincode: '',
    documentType: 'GST', referenceUrl: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apply({
        storeName: form.storeName,
        description: form.description,
        addressLine1: form.addressLine1,
        city: form.city,
        state: form.state,
        pincode: form.pincode,
        documents: [{ documentType: form.documentType, referenceUrl: form.referenceUrl }],
      });
      navigate('/merchant');
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not submit your application.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="onboarding-page">
      <div className="onboarding-copy">
        <StorefrontIllustration />
        <h1>Open your store on Vicinia</h1>
        <p>Tell us about your store — an admin will review your application, usually within a day.</p>
      </div>

      <form onSubmit={handleSubmit} className="card onboarding-form">
        {error && <div className="banner banner-error">{error}</div>}

        <div className="field">
          <label htmlFor="storeName">Store name</label>
          <input id="storeName" value={form.storeName} onChange={(e) => set('storeName', e.target.value)} required autoFocus />
        </div>
        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea id="description" rows={2} value={form.description} onChange={(e) => set('description', e.target.value)} required />
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
            <label htmlFor="state">State</label>
            <input id="state" value={form.state} onChange={(e) => set('state', e.target.value)} required />
          </div>
        </div>
        <div className="field">
          <label htmlFor="pincode">Pincode</label>
          <input id="pincode" value={form.pincode} onChange={(e) => set('pincode', e.target.value)} required />
        </div>

        <div className="field-row">
          <div className="field">
            <label htmlFor="documentType">Document type</label>
            <input id="documentType" value={form.documentType} onChange={(e) => set('documentType', e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="referenceUrl">Document URL</label>
            <input id="referenceUrl" type="url" placeholder="https://…" value={form.referenceUrl} onChange={(e) => set('referenceUrl', e.target.value)} required />
          </div>
        </div>

        <button className="btn btn-primary btn-block" disabled={submitting} style={{ marginTop: 8 }}>
          {submitting ? <span className="spinner" /> : 'Submit application'}
        </button>
      </form>
    </div>
  );
}
