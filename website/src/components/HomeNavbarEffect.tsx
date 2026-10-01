import {useEffect} from 'react';
import {useLocation} from '@docusaurus/router';

const SCROLL_THRESHOLD_PX = 24;

/** True when the first content section is a dark hero band. */
function pageHasTopHero(): boolean {
  const page = document.querySelector('.sc-page');
  if (!page) return false;
  const firstSection = page.querySelector(':scope > section');
  return Boolean(firstSection?.classList.contains('sc-hero-glow'));
}

/**
 * Pages with a top `sc-hero-glow` band: transparent navbar over hero → solid on scroll.
 * Pages without a hero keep the default solid/white navbar.
 * Uses data-* attrs (not className) so Docusaurus Helmet html className
 * updates don't wipe our state.
 */
export default function HomeNavbarEffect(): null {
  const {pathname} = useLocation();

  useEffect(() => {
    const root = document.documentElement;
    const isSearchPage =
      pathname === '/search' ||
      pathname === '/search/' ||
      pathname.endsWith('/search') ||
      pathname.endsWith('/search/');

    if (isSearchPage) {
      root.dataset.scSearch = '';
    } else {
      delete root.dataset.scSearch;
    }

    const apply = () => {
      const heroPage = pageHasTopHero();

      if (!heroPage) {
        delete root.dataset.scHome;
        delete root.dataset.scNavSolid;
        return;
      }

      root.dataset.scHome = '';

      if (window.scrollY > SCROLL_THRESHOLD_PX) {
        root.dataset.scNavSolid = '';
      } else {
        delete root.dataset.scNavSolid;
      }
    };

    apply();
    // Hash / late layout (MDX) may change first paint — re-check once settled.
    const raf = requestAnimationFrame(apply);
    const t = window.setTimeout(apply, 0);

    window.addEventListener('scroll', apply, {passive: true});
    window.addEventListener('resize', apply);

    return () => {
      cancelAnimationFrame(raf);
      window.clearTimeout(t);
      window.removeEventListener('scroll', apply);
      window.removeEventListener('resize', apply);
      delete root.dataset.scHome;
      delete root.dataset.scNavSolid;
      delete root.dataset.scSearch;
    };
  }, [pathname]);

  return null;
}
