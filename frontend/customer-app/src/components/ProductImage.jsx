import { useState } from 'react';

/** Falls back to a letter avatar on a missing image OR a broken URL (e.g. seed/test data's example.com placeholders, which 404) — plain `src ? <img> : fallback` only covers the first case. */
export default function ProductImage({ src, name, large }) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return <span className={`product-card-fallback ${large ? 'large' : ''}`}>{name?.charAt(0) || '?'}</span>;
  }

  return <img src={src} alt={name} loading="lazy" onError={() => setFailed(true)} />;
}
