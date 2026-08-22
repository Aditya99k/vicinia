import axios from 'axios';
import { getAuth, setAuth, clearAuth } from './storage';

/**
 * Default: derive the gateway URL from whatever host the browser actually
 * used to load this page — not a hardcoded IP. On a PC that's "localhost",
 * unaffected by network changes. On a phone it's whatever LAN IP (or
 * mDNS .local hostname) was typed into the address bar, so it keeps
 * working across Wi-Fi switches without ever touching this file or .env
 * again — the gateway just needs to be reachable at the same host on
 * port 8080, which it always is (see start-infra.sh).
 *
 * VITE_API_BASE_URL in .env is an explicit override for the rare case
 * that doesn't hold — e.g. a real deployed API domain later.
 */
const baseURL =
  import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8080`;

export const apiClient = axios.create({ baseURL });

apiClient.interceptors.request.use((config) => {
  const auth = getAuth();
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

// Concurrent 401s should trigger exactly one /refresh call, not one per
// failed request — every caller awaits the same in-flight promise.
let refreshPromise = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const isAuthEndpoint = originalRequest?.url?.startsWith('/api/auth/');

    if (status !== 401 || originalRequest?._retry || isAuthEndpoint) {
      return Promise.reject(error);
    }

    const auth = getAuth();
    if (!auth?.refreshToken || !auth?.userId) {
      clearAuth();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post(`${baseURL}/api/auth/refresh`, {
            userId: auth.userId,
            refreshToken: auth.refreshToken,
          })
          .then((res) => {
            setAuth(res.data);
            return res.data;
          })
          .finally(() => {
            refreshPromise = null;
          });
      }
      const refreshed = await refreshPromise;
      originalRequest.headers.Authorization = `Bearer ${refreshed.accessToken}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      clearAuth();
      window.location.href = '/login';
      return Promise.reject(refreshError);
    }
  }
);
