import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import CartAddedToast from './CartAddedToast';

export default function AppLayout() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-container">
        <Outlet />
      </main>
      <CartAddedToast />
    </div>
  );
}
