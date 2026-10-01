import {useCallback, useEffect, useId, useRef, useState, type ReactNode} from 'react';
import clsx from 'clsx';
import {Search, X} from 'lucide-react';
import SearchBar from '@theme/SearchBar';
import NavbarSearch from '@theme/Navbar/Search';

/**
 * Desktop: Metronic search field in the navbar.
 * Mobile: square Search icon button opens a fixed top search panel.
 */
export default function NavbarSearchControl(): ReactNode {
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const panelRef = useRef<HTMLDivElement>(null);

  const close = useCallback(() => setOpen(false), []);
  const openPanel = useCallback(() => setOpen(true), []);

  useEffect(() => {
    if (!open) return;

    const input = panelRef.current?.querySelector<HTMLInputElement>(
      '.navbar__search-input',
    );
    const t = window.setTimeout(() => {
      input?.focus();
      if (input && typeof input.setSelectionRange === 'function') {
        const len = input.value.length;
        input.setSelectionRange(len, len);
      }
    }, 0);

    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') close();
    };
    document.addEventListener('keydown', onKey);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      window.clearTimeout(t);
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
    };
  }, [open, close]);

  return (
    <>
      <button
        type="button"
        className="sc-navbar-search-btn"
        aria-label="Search"
        aria-expanded={open}
        aria-controls={panelId}
        title="Search"
        onClick={openPanel}
      >
        <Search size={16} strokeWidth={2.25} aria-hidden />
      </button>

      <div
        id={panelId}
        ref={panelRef}
        className={clsx('sc-navbar-search', open && 'sc-navbar-search--open')}
        data-sc-search-open={open ? 'true' : undefined}
      >
        <NavbarSearch className="sc-navbar-search__shell">
          <SearchBar />
        </NavbarSearch>
        <button
          type="button"
          className="sc-navbar-search-close"
          aria-label="Close search"
          title="Close"
          onClick={close}
        >
          <X size={18} strokeWidth={2.25} aria-hidden />
        </button>
      </div>

      {open ? (
        <button
          type="button"
          className="sc-navbar-search-backdrop"
          aria-label="Close search"
          onClick={close}
        />
      ) : null}
    </>
  );
}
