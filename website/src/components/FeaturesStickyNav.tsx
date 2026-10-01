import {useEffect, useRef, useState} from 'react';
import {ChevronDown} from 'lucide-react';
import {cn} from '../lib/utils';
import type {FeatureItem} from './features';

type FeaturesStickyNavProps = {
  items: Pick<FeatureItem, 'id' | 'navLabel'>[];
};

function scrollToFeature(id: string) {
  document.getElementById(id)?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  });
  history.replaceState(null, '', `#${id}`);
}

function navbarHeightPx() {
  return (
    parseFloat(
      getComputedStyle(document.documentElement).getPropertyValue(
        '--ifm-navbar-height',
      ),
    ) || 64
  );
}

/**
 * A+E Features nav:
 * - Appears + sticks only once the site header has scrolled over the section title
 * - Desktop (≥1024): full-bleed underline tabs
 * - Mobile (<1024): full-bleed compact select + progress
 * Active tab = last section whose top has passed under the sticky nav.
 */
export default function FeaturesStickyNav({items}: FeaturesStickyNavProps) {
  const [activeId, setActiveId] = useState(items[0]?.id ?? '');
  const [visible, setVisible] = useState(false);
  const navRef = useRef<HTMLElement>(null);
  const tabsRef = useRef<HTMLDivElement>(null);
  const lockUntilRef = useRef(0);

  const activeIndex = Math.max(
    0,
    items.findIndex(item => item.id === activeId),
  );

  // Reveal when site header passes the Features title; hide after section ends.
  useEffect(() => {
    const section = document.querySelector<HTMLElement>('.sc-features-section');
    if (!section) return;

    let raf = 0;

    const updateVisibility = () => {
      const headerH = navbarHeightPx();
      const title =
        section.querySelector<HTMLElement>('.sc-features-title') ??
        section.querySelector<HTMLElement>('.sc-features-heading');
      const titleTop = title
        ? title.getBoundingClientRect().top
        : section.getBoundingClientRect().top;

      // Header has scrolled over the title.
      const passedTitle = titleTop <= headerH;
      // Still inside Features (keep a bit of room for the fixed nav itself).
      const sectionBottom = section.getBoundingClientRect().bottom;
      const stillInSection = sectionBottom > headerH + 56;

      setVisible(passedTitle && stillInSection);
    };

    const onScrollOrResize = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(updateVisibility);
    };

    window.addEventListener('scroll', onScrollOrResize, {passive: true});
    window.addEventListener('resize', onScrollOrResize);
    updateVisibility();

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('scroll', onScrollOrResize);
      window.removeEventListener('resize', onScrollOrResize);
    };
  }, []);

  useEffect(() => {
    const sections = items
      .map(item => document.getElementById(item.id))
      .filter((el): el is HTMLElement => Boolean(el));

    if (sections.length === 0) return;

    let raf = 0;

    const probeY = () => {
      const nav = navRef.current;
      if (nav && visible) {
        return nav.getBoundingClientRect().bottom + 1;
      }
      return navbarHeightPx() + 48;
    };

    const updateActive = () => {
      if (performance.now() < lockUntilRef.current) return;

      const y = probeY();
      let nextId = sections[0].id;

      for (const section of sections) {
        if (section.getBoundingClientRect().top <= y) {
          nextId = section.id;
        } else {
          break;
        }
      }

      setActiveId(prev => (prev === nextId ? prev : nextId));
    };

    const onScrollOrResize = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(updateActive);
    };

    window.addEventListener('scroll', onScrollOrResize, {passive: true});
    window.addEventListener('resize', onScrollOrResize);
    updateActive();

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('scroll', onScrollOrResize);
      window.removeEventListener('resize', onScrollOrResize);
    };
  }, [items, visible]);

  useEffect(() => {
    if (!visible) return;

    const tabs = tabsRef.current;
    const active = tabs?.querySelector<HTMLElement>(
      `[data-feature-nav="${activeId}"]`,
    );
    if (!tabs || !active) return;

    const tabsRect = tabs.getBoundingClientRect();
    const activeRect = active.getBoundingClientRect();
    const outOfView =
      activeRect.left < tabsRect.left + 8 ||
      activeRect.right > tabsRect.right - 8;

    if (outOfView) {
      active.scrollIntoView({
        behavior: 'auto',
        inline: 'nearest',
        block: 'nearest',
      });
    }
  }, [activeId, visible]);

  const activate = (id: string) => {
    setActiveId(id);
    lockUntilRef.current = performance.now() + 900;
    scrollToFeature(id);
  };

  return (
    <nav
      ref={navRef}
      className={cn(
        'sc-features-nav',
        visible && 'sc-features-nav--visible',
      )}
      aria-label="Feature sections"
      aria-hidden={!visible}
    >
      <div className="sc-features-nav__inner">
        {/* A — Desktop underline tabs */}
        <div ref={tabsRef} className="sc-features-nav__tabs">
          {items.map(item => (
            <a
              key={item.id}
              href={`#${item.id}`}
              data-feature-nav={item.id}
              tabIndex={visible ? undefined : -1}
              className={cn(
                'sc-features-nav__tab',
                activeId === item.id && 'sc-features-nav__tab--active',
              )}
              onClick={e => {
                e.preventDefault();
                activate(item.id);
              }}
            >
              {item.navLabel}
            </a>
          ))}
        </div>

        {/* E — Mobile compact select */}
        <div className="sc-features-nav__mobile">
          <div className="sc-features-nav__mobile-meta">
            <span className="sc-features-nav__mobile-label">
              Jump to feature
            </span>
            <span className="sc-features-nav__mobile-progress">
              {activeIndex + 1} / {items.length}
            </span>
          </div>
          <div className="sc-features-nav__select-wrap">
            <select
              className="sc-features-nav__select"
              value={activeId}
              aria-label="Jump to feature"
              tabIndex={visible ? undefined : -1}
              onChange={e => activate(e.target.value)}
            >
              {items.map(item => (
                <option key={item.id} value={item.id}>
                  {item.navLabel}
                </option>
              ))}
            </select>
            <ChevronDown
              className="sc-features-nav__select-icon"
              size={16}
              strokeWidth={2}
              aria-hidden
            />
          </div>
        </div>
      </div>
    </nav>
  );
}
