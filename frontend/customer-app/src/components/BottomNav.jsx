import { NavLink } from 'react-router-dom';
import { HomeIcon, MapPinIcon, UserIcon } from './Icons';

const items = [
  { to: '/', label: 'Home', Icon: HomeIcon, end: true },
  { to: '/addresses', label: 'Addresses', Icon: MapPinIcon },
  { to: '/profile', label: 'Profile', Icon: UserIcon },
];

export default function BottomNav() {
  return (
    <nav className="bottomnav">
      {items.map(({ to, label, Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => `bottomnav-item${isActive ? ' active' : ''}`}
        >
          <Icon />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
