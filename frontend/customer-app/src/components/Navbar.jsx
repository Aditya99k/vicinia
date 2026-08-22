import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../hooks/useTheme';
import { MoonIcon, SunIcon } from './Icons';

export default function Navbar() {
  const { auth } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const initial = (auth?.email || '?').trim().charAt(0).toUpperCase();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <span className="mark">V</span>
          <span className="wordmark">Vicinia</span>
        </Link>

        <div className="navbar-search" title="Search arrives with catalog-service — Stage 4">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input placeholder="Search for atta, dal, oil & more" disabled />
        </div>

        <nav className="navbar-links">
          <NavLink to="/" end>Home</NavLink>
          <NavLink to="/addresses">Addresses</NavLink>
        </nav>

        <div className="navbar-actions">
          <button className="icon-btn" onClick={toggle} aria-label="Toggle color theme">
            {theme === 'dark' ? <SunIcon style={{ width: 18, height: 18 }} /> : <MoonIcon style={{ width: 18, height: 18 }} />}
          </button>
          <button className="navbar-avatar" onClick={() => navigate('/profile')} aria-label="Profile">
            {initial}
          </button>
        </div>
      </div>
    </header>
  );
}
