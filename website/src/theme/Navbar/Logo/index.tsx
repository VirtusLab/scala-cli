import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import ScalaCliWordmark from '../../../components/ScalaCliWordmark';

export default function NavbarLogo(): ReactNode {
  return (
    <Link
      to={useBaseUrl('/')}
      className="navbar__brand sc-navbar-brand"
      aria-label="Scala CLI home"
    >
      <ScalaCliWordmark className="sc-navbar-wordmark" />
    </Link>
  );
}
