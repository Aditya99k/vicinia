import { Outlet, useLocation } from 'react-router-dom';
import TopBar from './TopBar';
import BottomNav from './BottomNav';

const TITLES = {
  '/': { variant: 'brand' },
  '/addresses': { variant: 'title', title: 'Your addresses' },
  '/profile': { variant: 'title', title: 'Profile' },
};

export default function AppLayout() {
  const { pathname } = useLocation();
  const meta = TITLES[pathname] || { variant: 'title', title: 'Vicinia' };

  return (
    <div className="app-shell">
      <TopBar variant={meta.variant} title={meta.title} />
      <div className="app-content">
        <Outlet />
      </div>
      <BottomNav />
    </div>
  );
}
