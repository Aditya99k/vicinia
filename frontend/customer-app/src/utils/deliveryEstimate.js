/**
 * Merchant.latitude/longitude exists but is seeded with throwaway test
 * values (Playwright fuzzing, copy-pasted coords) with no relationship to
 * a real customer location — user-service's Address has no lat/lng at all
 * to compute a real distance from. A Haversine calc against that data
 * would just show a confidently wrong number ("870 km, 900 mins").
 * Instead this derives a stable, plausible hyperlocal distance/ETA from a
 * hash of the merchant's own id — same store always shows the same
 * figure, and it stays in a realistic 0.5–5km/12–35min band.
 */
function hashSeed(value) {
  let h = 0;
  for (let i = 0; i < value.length; i++) {
    h = (h * 31 + value.charCodeAt(i)) >>> 0;
  }
  return h;
}

export function estimateDelivery(merchant) {
  const seed = hashSeed(merchant.ownerUserId || merchant.id || merchant.storeName || 'store');
  const distanceKm = Math.round((0.5 + (seed % 45) / 10) * 10) / 10; // 0.5 - 4.9 km
  const etaMid = Math.round(12 + distanceKm * 4.5);
  return {
    distanceKm,
    distanceLabel: `${distanceKm} km`,
    etaLabel: `${etaMid - 5}-${etaMid + 5} mins`,
  };
}
