import { searchProducts } from '../api/catalog';
import { listingsForProduct } from '../api/inventory';

/**
 * inventory-service has no "listings for merchant X" endpoint customers can
 * call (see frontend/customer-app/README.md) — only "listings for product
 * X". To show each nearby store's own items on the home page, this walks
 * a capped sample of the catalog, fetches each product's listings, and
 * groups the active ones by merchantId client-side. Fine at this catalog's
 * size; a real "browse this store" endpoint would replace it at scale.
 *
 * <p>The grouping key is the listing's merchantId, which is the merchant's
 * *owner user ID* (stamped from the caller's auth identity when the listing
 * was created) — not merchant-service's own Merchant.id primary key that
 * `GET /api/merchants/nearby` returns as `id`. Callers must key their own
 * merchant list by `ownerUserId` to match these buckets; see HomePage.jsx.
 */
export async function loadStorefronts(merchants, { sampleSize = 20, perStore = 3 } = {}) {
  if (merchants.length === 0) return {};

  const products = await searchProducts({});
  const sample = products.slice(0, sampleSize);

  const results = await Promise.all(
    sample.map((product) =>
      listingsForProduct(product.id)
        .then((listings) => ({ product, listings }))
        .catch(() => ({ product, listings: [] }))
    )
  );

  const byMerchant = {};
  for (const { product, listings } of results) {
    for (const listing of listings) {
      if (!listing.active || listing.availableStock <= 0) continue;
      const bucket = (byMerchant[listing.merchantId] ||= []);
      if (bucket.length >= perStore) continue;
      if (bucket.some((item) => item.productId === product.id)) continue;
      bucket.push({ ...listing, productImage: product.images?.[0] });
    }
  }
  return byMerchant;
}
