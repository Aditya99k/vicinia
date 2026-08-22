const KEY = 'vicinia_auth';

/** Everything AuthResponse returns (userId, email, roles, permissions, accessToken, refreshToken, expiresInSeconds), one JSON blob. */
export function getAuth() {
  const raw = localStorage.getItem(KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setAuth(authResponse) {
  const merged = { ...(getAuth() || {}), ...authResponse };
  localStorage.setItem(KEY, JSON.stringify(merged));
  return merged;
}

export function clearAuth() {
  localStorage.removeItem(KEY);
}
