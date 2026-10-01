import type {ReactNode} from 'react';
import {useNavbarMobileSidebar} from '@docusaurus/theme-common/internal';
import {translate} from '@docusaurus/Translate';
import {X} from 'lucide-react';
import NavbarLogo from '@theme/Navbar/Logo';
import ThemeSwitch from '../../../../components/ThemeSwitch';

function CloseButton() {
  const mobileSidebar = useNavbarMobileSidebar();
  return (
    <button
      type="button"
      aria-label={translate({
        id: 'theme.docs.sidebar.closeSidebarButtonAriaLabel',
        message: 'Close navigation bar',
        description: 'The ARIA label for close button of mobile sidebar',
      })}
      title="Close"
      className="sc-navbar-sidebar-close"
      onClick={() => mobileSidebar.toggle()}
    >
      <X size={18} strokeWidth={2.25} aria-hidden />
    </button>
  );
}

export default function NavbarMobileSidebarHeader(): ReactNode {
  return (
    <div className="navbar-sidebar__brand sc-navbar-sidebar-brand">
      <NavbarLogo />
      <div className="sc-navbar-sidebar-brand__actions">
        <ThemeSwitch className="sc-navbar-theme" />
        <CloseButton />
      </div>
    </div>
  );
}
