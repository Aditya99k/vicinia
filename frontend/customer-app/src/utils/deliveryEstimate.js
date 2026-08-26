/**
 * merchant.distanceKm is a real value now, computed server-side
 * (GeoDistance.km in merchant-service) whenever the request carried the
 * customer's own coordinates — see api/merchant.js's nearby() and
 * MerchantService.nearby's own comment. It's only ever missing when the
 * caller didn't have a customer location to search from at all — the
 * unfiltered, system-wide directory useMerchantDirectory builds for
 * name-lookup purposes elsewhere (cart/order banners, store name on a
 * product listing), which has no per-request customer position and never
 * will. The hash-based placeholder below exists only for that remaining
 * case, so those banners still show a plausible, stable figure instead of
 * "NaN km" — never the primary "stores near me" experience, which always
 * has a real customer address by the time it calls nearby().
 */
function hashSeed(value) {
  let h = 0;
  for (let i = 0; i < value.length; i++) {
    h = (h * 31 + value.charCodeAt(i)) >>> 0;
  }
  return h;
}

function fakeEstimate(merchant) {
  const seed = hashSeed(merchant.ownerUserId || merchant.id || merchant.storeName || 'store');
  const distanceKm = Math.round((0.5 + (seed % 45) / 10) * 10) / 10; // 0.5 - 4.9 km
  const etaMid = Math.round(12 + distanceKm * 4.5);
  return {
    distanceKm,
    distanceLabel: `${distanceKm} km`,
    etaLabel: `${etaMid - 5}-${etaMid + 5} mins`,
  };
}

export function estimateDelivery(merchant) {
  if (merchant.distanceKm == null) return fakeEstimate(merchant);

  const distanceKm = Math.round(merchant.distanceKm * 10) / 10;
  // ~20 km/h effective delivery speed (city traffic, not door-to-door
  // straight-line time) plus a flat prep/handoff floor, same rough shape
  // as the placeholder it replaces so real distances don't suddenly look
  // wildly different in kind, just accurate instead of guessed.
  const etaMid = Math.round(12 + distanceKm * 3);
  return {
    distanceKm,
    distanceLabel: `${distanceKm} km`,
    etaLabel: `${Math.max(8, etaMid - 5)}-${etaMid + 5} mins`,
  };
}
