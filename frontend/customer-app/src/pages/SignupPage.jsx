import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { GroceryBagIllustration } from '../components/Illustrations';
import { homePath } from '../utils/roles';

const ROLES = [
  { value: 'CUSTOMER', label: 'Customer' },
  { value: 'MERCHANT', label: 'Merchant' },
  { value: 'DELIVERY_PARTNER', label: 'Delivery partner' },
];

export default function SignupPage() {
  const navigate = useNavigate();
  const { signup } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [role, setRole] = useState('CUSTOMER');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (password !== confirm) {
      setError("Passwords don't match.");
      return;
    }

    setLoading(true);
    try {
      const data = await signup({ email, password, role });
      navigate(homePath(data));
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not create your account.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-illustration-panel">
        <div className="badge-floating"><span className="dot" /> Fresh picks, every day</div>
        <GroceryBagIllustration />
        <div className="tagline">
          <h2>Join Vicinia</h2>
          <p>Create an account to shop from independent local merchants near you.</p>
        </div>
      </div>

      <div className="auth-form-panel">
        <div className="auth-brand">
          <span className="mark">V</span>
          <span className="wordmark">Vicinia</span>
        </div>

        <h1 className="auth-title">Create your account</h1>
        <p className="auth-sub">Fresh groceries and everyday essentials, from stores near you.</p>

        {error && <div className="banner banner-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="password">Password</label>
              <input id="password" type="password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="confirm">Confirm</label>
              <input id="confirm" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
            </div>
          </div>
          <span className="field-hint" style={{ display: 'block', marginBottom: 14 }}>At least 8 characters.</span>

          <div className="field">
            <label>I'm signing up as</label>
          </div>
          <div className="role-picker">
            {ROLES.map((r) => (
              <label key={r.value}>
                <input
                  className="role-pill-input"
                  type="radio"
                  name="role"
                  value={r.value}
                  checked={role === r.value}
                  onChange={() => setRole(r.value)}
                />
                <span className="role-pill">{r.label}</span>
              </label>
            ))}
          </div>

          <button className="btn btn-primary btn-block" disabled={loading}>
            {loading ? <span className="spinner" /> : 'Create account'}
          </button>
        </form>

        <div className="auth-switch">
          Already have an account? <Link to="/login">Log in</Link>
        </div>
      </div>
    </div>
  );
}
