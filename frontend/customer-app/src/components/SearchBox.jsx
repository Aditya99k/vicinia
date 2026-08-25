import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchProducts } from '../api/catalog';
import ProductImage from './ProductImage';
import { ChevronRightIcon } from './Icons';

/** Debounced live suggestions as you type — a separate small search (limit 6) from the full /search results page, not just a delayed version of it. */
export default function SearchBox({ initialQuery = '' }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState(initialQuery);
  const [suggestions, setSuggestions] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const wrapRef = useRef(null);
  const debounceRef = useRef(null);

  useEffect(() => {
    if (!query.trim()) {
      setSuggestions([]);
      return;
    }
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setLoading(true);
      searchProducts({ q: query.trim() })
        .then((results) => setSuggestions(results.slice(0, 6)))
        .catch(() => setSuggestions([]))
        .finally(() => setLoading(false));
    }, 250);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  useEffect(() => {
    function onOutsideClick(e) {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener('mousedown', onOutsideClick);
    return () => document.removeEventListener('mousedown', onOutsideClick);
  }, []);

  function goToProduct(id) {
    setOpen(false);
    navigate(`/product/${id}`);
  }

  function goToAllResults() {
    setOpen(false);
    if (query.trim()) navigate(`/search?q=${encodeURIComponent(query.trim())}`);
  }

  function handleSubmit(e) {
    e.preventDefault();
    goToAllResults();
  }

  function handleKeyDown(e) {
    if (e.key === 'Escape') setOpen(false);
  }

  return (
    <div className="navbar-search-wrap" ref={wrapRef}>
      <form className="navbar-search" onSubmit={handleSubmit}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
        <input
          placeholder="Search for atta, dal, oil & more"
          value={query}
          onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
        />
      </form>

      {open && query.trim() && (
        <div className="search-suggestions">
          {loading ? (
            <div className="search-suggestion-loading"><span className="spinner" /></div>
          ) : suggestions.length === 0 ? (
            <div className="search-suggestion-empty">No products match "{query.trim()}"</div>
          ) : (
            <>
              {suggestions.map((p) => (
                <button type="button" className="search-suggestion-row" key={p.id} onClick={() => goToProduct(p.id)}>
                  <div className="search-suggestion-thumb"><ProductImage src={p.images?.[0]} name={p.name} category={p.category} small /></div>
                  <div className="search-suggestion-body">
                    <div className="name">{p.name}</div>
                    <div className="muted">{p.brand} · {p.category}</div>
                  </div>
                </button>
              ))}
              <button type="button" className="search-suggestion-all" onClick={goToAllResults}>
                See all results for "{query.trim()}" <ChevronRightIcon style={{ width: 14, height: 14 }} />
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
