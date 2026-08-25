import { useEffect, useState } from 'react';
import { getProduct } from '../api/catalog';

/**
 * CartItemResponse and ListingResponse both carry productId/productName
 * but never an image or category (see frontend/customer-app/README.md) —
 * to show a thumbnail (and its category-glyph fallback) in the cart, this
 * resolves productId -> {image, category} by fetching each product once,
 * cached module-wide by id so navigating between pages never re-fetches
 * the same product twice.
 */
const cache = new Map();
const inflight = new Map();

export function useProductImages(productIds) {
  const [, forceRender] = useState(0);
  const key = productIds.filter(Boolean).sort().join(',');

  useEffect(() => {
    const missing = productIds.filter((id) => id && !cache.has(id) && !inflight.has(id));
    if (missing.length === 0) return;

    missing.forEach((id) => {
      const promise = getProduct(id)
        .then((p) => {
          cache.set(id, { image: p.images?.[0] || null, category: p.category || null });
        })
        .catch(() => {
          cache.set(id, { image: null, category: null });
        })
        .finally(() => {
          inflight.delete(id);
          forceRender((n) => n + 1);
        });
      inflight.set(id, promise);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return {
    imageFor: (productId) => cache.get(productId)?.image || null,
    categoryFor: (productId) => cache.get(productId)?.category || null,
  };
}
