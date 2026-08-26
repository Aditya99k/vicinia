import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import * as cartApi from '../api/cart';
import { useAuth } from './AuthContext';
import { primaryRole } from '../utils/roles';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { auth, isAuthenticated } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(false);
  const [lastAdded, setLastAdded] = useState(null);

  const refresh = useCallback(async () => {
    if (!isAuthenticated || primaryRole(auth) !== 'CUSTOMER') {
      setCart(null);
      return;
    }
    setLoading(true);
    try {
      const data = await cartApi.getCart();
      setCart(data);
    } catch {
      setCart(null);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, auth]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addItem = useCallback(async (listingId, quantity) => {
    const data = await cartApi.addItem({ listingId, quantity });
    setCart(data);
    const item = data.items?.find((i) => i.listingId === listingId);
    // A fresh object every time (even re-adding the same listing) so the
    // toast component's effect fires again instead of seeing an unchanged
    // reference and staying silent on a second add of the same item.
    setLastAdded(item ? { item, at: Date.now() } : null);
    return data;
  }, []);

  const updateItem = useCallback(async (listingId, quantity) => {
    const data = await cartApi.updateItem(listingId, quantity);
    setCart(data);
    return data;
  }, []);

  const removeItem = useCallback(async (listingId) => {
    const data = await cartApi.removeItem(listingId);
    setCart(data);
    return data;
  }, []);

  const clear = useCallback(async () => {
    const data = await cartApi.clearCart();
    setCart(data);
    return data;
  }, []);

  const itemCount = (cart?.items || []).reduce((sum, i) => sum + i.quantity, 0);

  const value = { cart, itemCount, loading, refresh, addItem, updateItem, removeItem, clear, lastAdded };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
