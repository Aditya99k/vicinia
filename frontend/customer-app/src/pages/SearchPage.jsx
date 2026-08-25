import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { searchProducts } from '../api/catalog';
import { EmptyBoxIllustration } from '../components/Illustrations';
import { ChevronDownIcon } from '../components/Icons';
import ProductImage from '../components/ProductImage';

export default function SearchPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') || '';
  const category = params.get('category') || '';

  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    searchProducts({ q: q || undefined, category: category || undefined })
      .then(setProducts)
      .catch(() => setError('Could not load products.'))
      .finally(() => setLoading(false));
  }, [q, category]);

  function clearCategory() {
    const next = new URLSearchParams(params);
    next.delete('category');
    setParams(next);
  }

  return (
    <div>
      <div className="addresses-header">
        <div>
          <h1>{q ? `Results for "${q}"` : category || 'Browse products'}</h1>
          <p>{loading ? 'Searching…' : `${products.length} product${products.length === 1 ? '' : 's'} found`}</p>
        </div>
        {category && (
          <button className="btn btn-secondary btn-sm" onClick={clearCategory}>
            Clear category ✕
          </button>
        )}
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : products.length === 0 ? (
        <div className="empty-state">
          <EmptyBoxIllustration />
          <h3>No products found</h3>
          <p>Try a different search term or browse another category from the home page.</p>
        </div>
      ) : (
        <div className="product-grid">
          {products.map((p) => (
            <Link to={`/product/${p.id}`} className="product-card" key={p.id}>
              <div className="product-card-image">
                <ProductImage src={p.images?.[0]} name={p.name} />
              </div>
              <div className="product-card-body">
                <div className="brand">{p.brand}</div>
                <div className="name">{p.name}</div>
                <div className="category-tag">{p.category}</div>
              </div>
              <div className="product-card-cta">
                View offers <ChevronDownIcon style={{ width: 14, height: 14, transform: 'rotate(-90deg)' }} />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
