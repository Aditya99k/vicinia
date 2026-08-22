import axios from 'axios';
import { getAuth, setAuth, clearAuth } from './storage';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

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
