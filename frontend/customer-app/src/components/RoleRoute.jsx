import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { homePath } from '../utils/roles';

/** Like ProtectedRoute, but also redirects a logged-in user away from another role's area to their own home — a merchant hitting /admin lands on /merchant, not a 403 page. */
export default function RoleRoute({ role, children }) {
  const { auth, isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!(auth?.roles || []).includes(role)) return <Navigate to={homePath(auth)} replace />;
  return children;
}
