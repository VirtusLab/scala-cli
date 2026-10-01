import {useEffect, useRef, useState} from 'react';
import {ChevronDown} from 'lucide-react';
import {cn} from '../lib/utils';

export type InstallTocItem = {
  id: string;
  navLabel: string;
};

type InstallStickyNavProps = {
  items: InstallTocItem[];
};

function scrollToSection(id: string) {
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
 * Install page jump TOC — fixed under the navbar after the Quick start hero,
 * hidden again near the footer.
 */
export default function InstallStickyNav({items}: InstallStickyNavProps) {
  const [activeId, setActiveId] = useState(items[0]?.id ?? '');
  const [visible, setVisible] = useState(false);
  const navRef = useRef<HTMLElement>(null);
  const tabsRef = useRef<HTMLDivElement>(null);
  const lockUntilRef = useRef(0);

  const activeIndex = Math.max(
    0,
    items.findIndex(item => item.id === activeId),
  );

  useEffect(() => {
    const page = document.querySelector<HTMLElement>('.sc-install-page');
    if (!page) return;

    let raf = 0;

    const updateVisibility = () => {
      const headerH = navbarHeightPx();
      const hero =
        page.querySelector<HTMLElement>('.sc-hero-glow') ??
        document.getElementById('quick-start');
      if (!hero) {
        setVisible(false);
        return;
      }

      const passedHero = hero.getBoundingClientRect().bottom <= headerH + 8;
      const pageBottom = page.getBoundingClientRect().bottom;
      const stillOnPage = pageBottom > headerH + 56;

      setVisible(passedHero && stillOnPage);
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
      `[data-install-nav="${activeId}"]`,
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
    scrollToSection(id);
  };

  return (
    <nav
      ref={navRef}
      className={cn('sc-jump-nav', visible && 'sc-jump-nav--visible')}
      aria-label="Install sections"
      aria-hidden={!visible}
    >
      <div className="sc-jump-nav__inner">
        <div ref={tabsRef} className="sc-jump-nav__tabs">
          {items.map(item => (
            <a
              key={item.id}
              href={`#${item.id}`}
              data-install-nav={item.id}
              tabIndex={visible ? undefined : -1}
              className={cn(
                'sc-jump-nav__tab',
                activeId === item.id && 'sc-jump-nav__tab--active',
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

        <div className="sc-jump-nav__mobile">
          <div className="sc-jump-nav__mobile-meta">
            <span className="sc-jump-nav__mobile-label">Jump to section</span>
            <span className="sc-jump-nav__mobile-progress">
              {activeIndex + 1} / {items.length}
            </span>
          </div>
          <div className="sc-jump-nav__select-wrap">
            <select
              className="sc-jump-nav__select"
              value={activeId}
              aria-label="Jump to section"
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
              className="sc-jump-nav__select-icon"
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
