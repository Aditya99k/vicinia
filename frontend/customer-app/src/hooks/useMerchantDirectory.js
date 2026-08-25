import { useEffect, useState } from 'react';
import { nearby } from '../api/merchant';

/**
 * merchant-service has no "get merchant by id" lookup — only /nearby (a
 * list, filterable by city) and /me (self only). To show a shop's name
 * anywhere a cart/order only carries its merchantId (which is actually the
 * owner's user id — see MerchantSummaryResponse's own comment), the whole
 * app shares one client-side directory: fetch every LIVE merchant once
 * (no city filter = system-wide, per MerchantService.nearby), keyed by
 * ownerUserId, and reuse it everywhere rather than re-fetching per page.
 */
let cache = null;
let inflight = null;

function loadDirectory() {
  if (cache) return Promise.resolve(cache);
  if (!inflight) {
    inflight = nearby()
      .then((list) => {
        cache = new Map(list.map((m) => [m.ownerUserId, m]));
        return cache;
      })
      .finally(() => {
        inflight = null;
      });
  }
  return inflight;
}

export function useMerchantDirectory() {
  const [directory, setDirectory] = useState(cache);

  useEffect(() => {
    if (directory) return;
    let cancelled = false;
    loadDirectory().then((d) => {
      if (!cancelled) setDirectory(d);
    });
    return () => {
      cancelled = true;
    };
  }, [directory]);

  return directory || new Map();
}
