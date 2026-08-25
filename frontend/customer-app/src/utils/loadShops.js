import { listingsForMerchant } from '../api/inventory';

/**
 * One real fetch per nearby merchant (there are only ever a handful LIVE
 * at once in practice) rather than the old sampled-product join — gives an
 * accurate active-item count per store instead of guessing from a capped
 * catalog sample. Stores with nothing to sell are dropped so "shops near
 * you" only ever links to a shop with something in it.
 */
export async function loadShops(merchants) {
  const results = await Promise.all(
    merchants.map(async (merchant) => {
      try {
        const listings = await listingsForMerchant(merchant.ownerUserId);
        const active = listings.filter((l) => l.active && l.availableStock > 0);
        return { merchant, itemCount: active.length };
      } catch {
        return { merchant, itemCount: 0 };
      }
    })
  );
  return results.filter((s) => s.itemCount > 0);
}
