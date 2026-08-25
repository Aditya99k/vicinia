import { searchProducts } from '../api/catalog';
import { listingsForProduct } from '../api/inventory';

/**
 * inventory-service has no "listings for merchant X" endpoint customers can
 * call (see frontend/customer-app/README.md) — only "listings for product
 * X". To show a Blinkit-style "items near you" feed on the home page, this
 * walks a capped sample of the catalog, fetches each product's listings,
 * and flattens the active ones from LIVE merchants into individual cards
 * (one per listing, not grouped by store) — each card names its own store,
 * since a listing IS one merchant's price/stock for that product. Fine at
 * this catalog's size; a real "browse nearby listings" endpoint would
 * replace it at scale.
 *
 * <p>Matching a listing's merchantId to a store name needs `merchants`
 * keyed the same way this project's ID quirk requires everywhere else: by
 * `ownerUserId` (what inventory-service actually stamps as merchantId),
 * not merchant-service's own `Merchant.id` that `/nearby` returns as `id`.
 */
export async function loadNearbyListings(merchants, { sampleSize = 24, limit = 18 } = {}) {
  if (merchants.length === 0) return [];

  const byOwnerUserId = new Map(merchants.map((m) => [m.ownerUserId, m]));
  const products = await searchProducts({});
  const sample = products.slice(0, sampleSize);

  const results = await Promise.all(
    sample.map((product) =>
      listingsForProduct(product.id)
        .then((listings) => ({ product, listings }))
        .catch(() => ({ product, listings: [] }))
    )
  );

  const items = [];
  for (const { product, listings } of results) {
    for (const listing of listings) {
      if (!listing.active || listing.availableStock <= 0) continue;
      const merchant = byOwnerUserId.get(listing.merchantId);
      if (!merchant) continue; // only show listings from merchants actually in the nearby list
      items.push({
        ...listing,
        productImage: product.images?.[0],
        productBrand: product.brand,
        storeName: merchant.storeName,
      });
      if (items.length >= limit) return items;
    }
  }
  return items;
}
