import { useMerchantDirectory } from '../hooks/useMerchantDirectory';
import { StoreIcon } from './Icons';

/** Shop attribution shown consistently on cart, checkout, and order detail — resolves a merchantId to a store name via the shared merchant directory. */
export default function ShopBanner({ merchantId }) {
  const directory = useMerchantDirectory();
  const storeName = directory.get(merchantId)?.storeName;

  if (!storeName) return null;

  return (
    <div className="shop-banner">
      <div className="merchant-tile-icon"><StoreIcon /></div>
      <div>
        <div className="eyebrow">Ordering from</div>
        <div className="name">{storeName}</div>
      </div>
    </div>
  );
}
