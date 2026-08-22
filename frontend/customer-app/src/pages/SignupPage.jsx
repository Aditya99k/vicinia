import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

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
      await signup({ email, password, role });
      navigate('/');
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not create your account.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-brand">
        <span className="mark">V</span>
        <span className="wordmark">Vicinia</span>
      </div>

      <div className="auth-card">
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-sub">Fresh groceries and everyday essentials, from stores near you.</p>

        {error && <div className="banner banner-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" type="password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
            <span className="field-hint">At least 8 characters.</span>
          </div>
          <div className="field">
            <label htmlFor="confirm">Confirm password</label>
            <input id="confirm" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
          </div>

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
