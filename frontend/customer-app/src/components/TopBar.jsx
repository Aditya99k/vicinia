import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function TopBar({ variant = 'title', title }) {
  const navigate = useNavigate();
  const { auth } = useAuth();
  const initial = (auth?.email || '?').trim().charAt(0).toUpperCase();

  return (
    <header className="topbar">
      <div className="topbar-deliver">
        {variant === 'brand' ? (
          <>
            <span className="label">Vicinia</span>
            <span className="value">Hello{auth?.email ? `, ${auth.email.split('@')[0]}` : ''} 👋</span>
          </>
        ) : (
          <span className="value" style={{ fontSize: 17 }}>{title}</span>
        )}
      </div>
      <button className="topbar-avatar" onClick={() => navigate('/profile')} aria-label="Profile">
        {initial}
      </button>
    </header>
  );
}
