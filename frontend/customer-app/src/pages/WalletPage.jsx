import { useEffect, useState } from 'react';
import { getWalletBalance, topupWallet } from '../api/payment';
import { WalletIcon } from '../components/Icons';
import { formatMoney } from '../utils/format';

const QUICK_AMOUNTS = [100, 250, 500, 1000];

export default function WalletPage() {
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  function load() {
    setLoading(true);
    getWalletBalance().then(setBalance).catch(() => setError('Could not load your wallet.')).finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleTopup(e) {
    e.preventDefault();
    const value = Number(amount);
    if (!value || value <= 0) return;
    setSubmitting(true);
    setError('');
    setSuccess(false);
    try {
      await topupWallet(value);
      setAmount('');
      setSuccess(true);
      load();
      setTimeout(() => setSuccess(false), 3000);
    } catch {
      setError('Could not complete this top-up.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="profile-page">
      <div className="wallet-hero">
        <div>
          <span className="eyebrow">Wallet balance</span>
          {loading ? (
            <div className="page-loading" style={{ justifyContent: 'flex-start', padding: '8px 0' }}><span className="spinner" /></div>
          ) : (
            <h1 style={{ fontSize: 32, marginTop: 4 }}>{formatMoney(balance?.balance)}</h1>
          )}
        </div>
        <div className="wallet-icon"><WalletIcon style={{ width: 26, height: 26 }} /></div>
      </div>

      <div className="section-title"><span>Top up</span></div>
      <div className="card">
        {error && <div className="banner banner-error">{error}</div>}
        {success && <div className="banner banner-success">Wallet topped up successfully.</div>}
        <form onSubmit={handleTopup}>
          <div className="field">
            <label htmlFor="amount">Amount</label>
            <input id="amount" type="number" min="1" step="0.01" placeholder="0.00" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          </div>
          <div className="chip-row" style={{ marginBottom: 16 }}>
            {QUICK_AMOUNTS.map((a) => (
              <button type="button" key={a} className="badge badge-muted" style={{ cursor: 'pointer', border: 'none' }} onClick={() => setAmount(String(a))}>
                +{formatMoney(a)}
              </button>
            ))}
          </div>
          <button className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? <span className="spinner" /> : 'Add money'}
          </button>
        </form>
      </div>
    </div>
  );
}
