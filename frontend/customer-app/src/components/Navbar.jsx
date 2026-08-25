import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useTheme } from '../hooks/useTheme';
import { homePathForRole, primaryRole } from '../utils/roles';
import { CartIcon, MoonIcon, SunIcon } from './Icons';
import SearchBox from './SearchBox';
import NotificationsBell from './NotificationsBell';

const SECTION_LINKS = {
  CUSTOMER: [
    { to: '/', label: 'Home', end: true },
    { to: '/orders', label: 'Orders' },
    { to: '/wallet', label: 'Wallet' },
    { to: '/addresses', label: 'Addresses' },
  ],
  MERCHANT: [
    { to: '/merchant', label: 'Dashboard', end: true },
    { to: '/merchant/listings', label: 'Listings' },
    { to: '/merchant/orders', label: 'Orders' },
    { to: '/merchant/settlements', label: 'Settlements' },
    { to: '/merchant/store', label: 'Store' },
  ],
  DELIVERY_PARTNER: [
    { to: '/delivery', label: 'Tasks', end: true },
    { to: '/delivery/history', label: 'History' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Overview', end: true },
    { to: '/admin/merchants', label: 'Merchants' },
    { to: '/admin/products', label: 'Products' },
    { to: '/admin/coupons', label: 'Coupons' },
    { to: '/admin/settlements', label: 'Settlements' },
  ],
};

function sectionForPath(pathname) {
  if (pathname.startsWith('/merchant')) return 'MERCHANT';
  if (pathname.startsWith('/delivery')) return 'DELIVERY_PARTNER';
  if (pathname.startsWith('/admin')) return 'ADMIN';
  return 'CUSTOMER';
}

export default function Navbar() {
  const { auth } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const initial = (auth?.email || '?').trim().charAt(0).toUpperCase();

  const role = primaryRole(auth);
  const section = sectionForPath(location.pathname);
  const links = SECTION_LINKS[section] || SECTION_LINKS.CUSTOMER;
  const showCommerceChrome = section === 'CUSTOMER';

  const { itemCount } = useCart();
  const initialQuery = new URLSearchParams(location.search).get('q') || '';

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to={homePathForRole(role)} className="navbar-brand">
          <span className="mark">V</span>
          <span className="wordmark">Vicinia</span>
        </Link>

        {showCommerceChrome && <SearchBox key={location.pathname} initialQuery={initialQuery} />}

        <nav className="navbar-links">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end}>{l.label}</NavLink>
          ))}
        </nav>

        <div className="navbar-actions">
          {showCommerceChrome && (
            <button className="icon-btn cart-btn" onClick={() => navigate('/cart')} aria-label="Cart">
              <CartIcon style={{ width: 18, height: 18 }} />
              {itemCount > 0 && <span className="cart-count">{itemCount}</span>}
            </button>
          )}
          <NotificationsBell />
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
