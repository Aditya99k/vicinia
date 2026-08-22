import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getProfile, updateProfile } from '../api/user';
import { LogoutIcon } from '../components/Icons';

export default function ProfilePage() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getProfile()
      .then((p) => {
        setProfile(p);
        setFullName(p.fullName || '');
        setPhone(p.phone || '');
      })
      .catch((err) => {
        if (err?.response?.status !== 404) {
          setError('Could not load your profile.');
        }
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(e) {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSaved(false);
    try {
      const updated = await updateProfile({ fullName, phone });
      setProfile(updated);
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch {
      setError('Could not save your changes.');
    } finally {
      setSaving(false);
    }
  }

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  const initial = (auth?.email || '?').trim().charAt(0).toUpperCase();

  return (
    <div className="profile-page">
      <div className="profile-header">
        <div className="profile-avatar">{initial}</div>
        <div>
          <h2>{profile?.fullName || 'Your profile'}</h2>
          <div className="email">{auth?.email}</div>
        </div>
      </div>

      <div className="section-title"><span>Roles &amp; permissions</span></div>
      <div className="chip-row" style={{ marginBottom: 20 }}>
        {(auth?.roles || []).map((r) => (
          <span key={r} className="badge">{r}</span>
        ))}
        {(auth?.permissions || []).map((p) => (
          <span key={p} className="badge badge-muted">{p}</span>
        ))}
      </div>

      <div className="section-title"><span>Edit details</span></div>

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : (
        <form onSubmit={handleSave} className="card">
          {error && <div className="banner banner-error">{error}</div>}
          {saved && <div className="banner banner-success">Saved.</div>}
          <div className="field">
            <label htmlFor="fullName">Full name</label>
            <input id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Add your name" />
          </div>
          <div className="field" style={{ marginBottom: 8 }}>
            <label htmlFor="phone">Phone</label>
            <input id="phone" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91-9xxxxxxxxx" />
          </div>
          <button className="btn btn-primary btn-block" disabled={saving} style={{ marginTop: 8 }}>
            {saving ? <span className="spinner" /> : 'Save changes'}
          </button>
        </form>
      )}

      <button className="btn btn-secondary btn-block" onClick={handleLogout} style={{ marginTop: 24, color: 'var(--danger)' }}>
        <LogoutIcon style={{ width: 17, height: 17 }} />
        Log out
      </button>
    </div>
  );
}
