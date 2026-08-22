import { createContext, useCallback, useContext, useState } from 'react';
import * as authApi from '../api/auth';
import { clearAuth, getAuth, setAuth } from '../api/storage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuthState] = useState(() => getAuth());

  const signup = useCallback(async (payload) => {
    const data = await authApi.signup(payload);
    setAuth(data);
    setAuthState(data);
    return data;
  }, []);

  const login = useCallback(async (payload) => {
    const data = await authApi.login(payload);
    setAuth(data);
    setAuthState(data);
    return data;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // best-effort — still clear local state even if the network call fails
    }
    clearAuth();
    setAuthState(null);
  }, []);

  const value = {
    auth,
    isAuthenticated: Boolean(auth?.accessToken),
    signup,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
