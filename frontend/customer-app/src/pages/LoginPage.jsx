import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as authApi from '../api/auth';

function extractError(err, fallback) {
  return err?.response?.data?.error || fallback;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [mode, setMode] = useState('login'); // login | forgot | reset
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function handleLogin(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login({ email, password });
      navigate('/');
    } catch (err) {
      setError(extractError(err, 'Could not log in — check your email and password.'));
    } finally {
      setLoading(false);
    }
  }

  async function handleForgot(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authApi.forgotPassword(email);
      setNotice(
        "If that email is registered, a reset link was created. Since notification-service isn't wired up yet, check auth-service's own log output (logs/auth-service.log) for the token — then paste it below."
      );
      setMode('reset');
    } catch (err) {
      setError(extractError(err, 'Something went wrong.'));
    } finally {
      setLoading(false);
    }
  }

  async function handleReset(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authApi.resetPassword({ token: resetToken, newPassword });
      setNotice('Password updated — log in with your new password.');
      setMode('login');
      setPassword('');
    } catch (err) {
      setError(extractError(err, 'That reset token is invalid or expired.'));
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
        {mode === 'login' && (
          <>
            <h1 className="auth-title">Welcome back</h1>
            <p className="auth-sub">Log in to keep shopping from stores near you.</p>
            {notice && <div className="banner banner-success">{notice}</div>}
            {error && <div className="banner banner-error">{error}</div>}
            <form onSubmit={handleLogin}>
              <div className="field">
                <label htmlFor="email">Email</label>
                <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
              </div>
              <div className="field">
                <label htmlFor="password">Password</label>
                <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
              </div>
              <div className="auth-forgot">
                <button type="button" onClick={() => { setMode('forgot'); setError(''); setNotice(''); }}>
                  Forgot password?
                </button>
              </div>
              <button className="btn btn-primary btn-block" disabled={loading}>
                {loading ? <span className="spinner" /> : 'Log in'}
              </button>
            </form>
            <div className="auth-switch">
              New to Vicinia? <Link to="/signup">Create an account</Link>
            </div>
          </>
        )}

        {mode === 'forgot' && (
          <>
            <h1 className="auth-title">Reset your password</h1>
            <p className="auth-sub">Enter the email on your account.</p>
            {error && <div className="banner banner-error">{error}</div>}
            <form onSubmit={handleForgot}>
              <div className="field">
                <label htmlFor="forgot-email">Email</label>
                <input id="forgot-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
              </div>
              <button className="btn btn-primary btn-block" disabled={loading}>
                {loading ? <span className="spinner" /> : 'Send reset link'}
              </button>
            </form>
            <div className="auth-switch">
              <button onClick={() => setMode('login')}>Back to login</button>
            </div>
          </>
        )}

        {mode === 'reset' && (
          <>
            <h1 className="auth-title">Set a new password</h1>
            {notice && <div className="banner banner-success">{notice}</div>}
            {error && <div className="banner banner-error">{error}</div>}
            <form onSubmit={handleReset}>
              <div className="field">
                <label htmlFor="token">Reset token</label>
                <input id="token" value={resetToken} onChange={(e) => setResetToken(e.target.value)} required autoFocus />
              </div>
              <div className="field">
                <label htmlFor="new-password">New password</label>
                <input id="new-password" type="password" minLength={8} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
              </div>
              <button className="btn btn-primary btn-block" disabled={loading}>
                {loading ? <span className="spinner" /> : 'Update password'}
              </button>
            </form>
            <div className="auth-switch">
              <button onClick={() => setMode('login')}>Back to login</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
