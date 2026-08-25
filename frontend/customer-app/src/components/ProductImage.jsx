import { useState } from 'react';
import CategoryGlyphFor from './CategoryGlyphFor';

/**
 * Falls back to a category glyph (the same hand-drawn icon set "shop by
 * category" uses) on a missing image OR a broken URL (e.g. seed/test
 * data's example.com placeholders, which 404) — plain `src ? <img> :
 * fallback` only covers the first case. Falls back further to a plain
 * letter badge only when no category is known either.
 */
export default function ProductImage({ src, name, category, large, small }) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    const size = large ? 'large' : small ? 'small' : '';
    return (
      <span className={`product-card-fallback ${size}`}>
        {category ? <CategoryGlyphFor name={category} /> : <span className="letter">{name?.charAt(0) || '?'}</span>}
      </span>
    );
  }

  return <img src={src} alt={name} loading="lazy" onError={() => setFailed(true)} />;
}
