import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';
import AppLayout from './components/AppLayout';

import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';

import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import StorePage from './pages/StorePage';
import ProductPage from './pages/ProductPage';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import OrdersPage from './pages/OrdersPage';
import OrderDetailPage from './pages/OrderDetailPage';
import WalletPage from './pages/WalletPage';
import ProfilePage from './pages/ProfilePage';
import AddressesPage from './pages/AddressesPage';

import MerchantDashboardPage from './pages/merchant/MerchantDashboardPage';
import MerchantApplyPage from './pages/merchant/MerchantApplyPage';
import MerchantStorePage from './pages/merchant/MerchantStorePage';
import MerchantListingsPage from './pages/merchant/MerchantListingsPage';
import MerchantOrdersPage from './pages/merchant/MerchantOrdersPage';
import MerchantSettlementsPage from './pages/merchant/MerchantSettlementsPage';

import DeliveryHomePage from './pages/delivery/DeliveryHomePage';
import DeliveryHistoryPage from './pages/delivery/DeliveryHistoryPage';

import AdminOverviewPage from './pages/admin/AdminOverviewPage';
import AdminMerchantsPage from './pages/admin/AdminMerchantsPage';
import AdminProductsPage from './pages/admin/AdminProductsPage';
import AdminCouponsPage from './pages/admin/AdminCouponsPage';
import AdminSettlementsPage from './pages/admin/AdminSettlementsPage';

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />

            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              {/* Customer */}
              <Route path="/" element={<HomePage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/store/:merchantId" element={<StorePage />} />
              <Route path="/product/:id" element={<ProductPage />} />
              <Route path="/cart" element={<CartPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/orders" element={<OrdersPage />} />
              <Route path="/orders/:id" element={<OrderDetailPage />} />
              <Route path="/wallet" element={<WalletPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/addresses" element={<AddressesPage />} />

              {/* Merchant */}
              <Route path="/merchant" element={<RoleRoute role="MERCHANT"><MerchantDashboardPage /></RoleRoute>} />
              <Route path="/merchant/apply" element={<RoleRoute role="MERCHANT"><MerchantApplyPage /></RoleRoute>} />
              <Route path="/merchant/store" element={<RoleRoute role="MERCHANT"><MerchantStorePage /></RoleRoute>} />
              <Route path="/merchant/listings" element={<RoleRoute role="MERCHANT"><MerchantListingsPage /></RoleRoute>} />
              <Route path="/merchant/orders" element={<RoleRoute role="MERCHANT"><MerchantOrdersPage /></RoleRoute>} />
              <Route path="/merchant/settlements" element={<RoleRoute role="MERCHANT"><MerchantSettlementsPage /></RoleRoute>} />

              {/* Delivery */}
              <Route path="/delivery" element={<RoleRoute role="DELIVERY_PARTNER"><DeliveryHomePage /></RoleRoute>} />
              <Route path="/delivery/history" element={<RoleRoute role="DELIVERY_PARTNER"><DeliveryHistoryPage /></RoleRoute>} />

              {/* Admin */}
              <Route path="/admin" element={<RoleRoute role="ADMIN"><AdminOverviewPage /></RoleRoute>} />
              <Route path="/admin/merchants" element={<RoleRoute role="ADMIN"><AdminMerchantsPage /></RoleRoute>} />
              <Route path="/admin/products" element={<RoleRoute role="ADMIN"><AdminProductsPage /></RoleRoute>} />
              <Route path="/admin/coupons" element={<RoleRoute role="ADMIN"><AdminCouponsPage /></RoleRoute>} />
              <Route path="/admin/settlements" element={<RoleRoute role="ADMIN"><AdminSettlementsPage /></RoleRoute>} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </CartProvider>
    </AuthProvider>
  );
}
