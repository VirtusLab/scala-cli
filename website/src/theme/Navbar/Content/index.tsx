import React, {type ReactNode} from 'react';
import clsx from 'clsx';
import {
  useThemeConfig,
  ErrorCauseBoundary,
  ThemeClassNames,
} from '@docusaurus/theme-common';
import {
  splitNavbarItems,
  useNavbarMobileSidebar,
} from '@docusaurus/theme-common/internal';
import NavbarItem, {type Props as NavbarItemConfig} from '@theme/NavbarItem';
import NavbarMobileSidebarToggle from '@theme/Navbar/MobileSidebar/Toggle';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import NavbarDevTools from '../../../components/NavbarDevTools';
import ThemeSwitch from '../../../components/ThemeSwitch';
import ScalaCliWordmark from '../../../components/ScalaCliWordmark';
import NavbarSearchControl from '../../../components/NavbarSearchControl';

import styles from './styles.module.css';

function useNavbarItems() {
  return useThemeConfig().navbar.items as NavbarItemConfig[];
}

function NavbarItems({items}: {items: NavbarItemConfig[]}): ReactNode {
  return (
    <>
      {items.map((item, i) => (
        <ErrorCauseBoundary
          key={i}
          onError={(error) =>
            new Error(
              `A theme navbar item failed to render.
Please double-check the following navbar item (themeConfig.navbar.items) of your Docusaurus config:
${JSON.stringify(item, null, 2)}`,
              {cause: error},
            )
          }
        >
          <NavbarItem {...item} />
        </ErrorCauseBoundary>
      ))}
    </>
  );
}

function NavbarContentLayout({
  left,
  right,
}: {
  left: ReactNode;
  right: ReactNode;
}) {
  return (
    <div className="navbar__inner sc-navbar-inner">
      <div
        className={clsx(
          ThemeClassNames.layout.navbar.containerLeft,
          'navbar__items',
        )}
      >
        {left}
      </div>
      <div
        className={clsx(
          ThemeClassNames.layout.navbar.containerRight,
          'navbar__items navbar__items--right sc-navbar-right',
        )}
      >
        {right}
      </div>
    </div>
  );
}

export default function NavbarContent(): ReactNode {
  const mobileSidebar = useNavbarMobileSidebar();
  const items = useNavbarItems();
  const [leftItems, rightItems] = splitNavbarItems(items);
  const searchBarItem = items.find(item => item.type === 'search');
  const homeLink = useBaseUrl('/');

  return (
    <NavbarContentLayout
      left={
        <>
          {!mobileSidebar.disabled && <NavbarMobileSidebarToggle />}
          <Link
            to={homeLink}
            className="navbar__brand sc-navbar-brand"
            aria-label="Scala CLI home"
          >
            <ScalaCliWordmark className="sc-navbar-wordmark" />
          </Link>
          <NavbarItems items={leftItems} />
        </>
      }
      right={
        <>
          <NavbarItems items={rightItems} />
          {!searchBarItem && <NavbarSearchControl />}
          <NavbarDevTools />
          <span className="sc-navbar-sep" aria-hidden />
          <ThemeSwitch className={clsx(styles.colorModeToggle, 'sc-navbar-theme')} />
        </>
      }
    />
  );
}
