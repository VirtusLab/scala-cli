import React, {type ReactNode} from 'react';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import {translate} from '@docusaurus/Translate';
import {House} from 'lucide-react';

export default function HomeBreadcrumbItem(): ReactNode {
  const homeHref = useBaseUrl('/');

  return (
    <li className="breadcrumbs__item">
      <Link
        aria-label={translate({
          id: 'theme.docs.breadcrumbs.home',
          message: 'Home page',
          description: 'The ARIA label for the home page in the breadcrumbs',
        })}
        className="breadcrumbs__link breadcrumbs__link--home"
        href={homeHref}>
        <House className="breadcrumbs__home-icon" aria-hidden size={15} strokeWidth={2} />
      </Link>
    </li>
  );
}
