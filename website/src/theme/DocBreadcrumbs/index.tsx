import React, {type ReactNode} from 'react';
import clsx from 'clsx';
import {ThemeClassNames} from '@docusaurus/theme-common';
import {
  useSidebarBreadcrumbs,
  findFirstSidebarItemLink,
} from '@docusaurus/plugin-content-docs/client';
import {useHomePageRoute} from '@docusaurus/theme-common/internal';
import Link from '@docusaurus/Link';
import {translate} from '@docusaurus/Translate';
import HomeBreadcrumbItem from '@theme/DocBreadcrumbs/Items/Home';
import DocBreadcrumbsStructuredData from '@theme/DocBreadcrumbs/StructuredData';
import type {PropSidebarBreadcrumbsItem} from '@docusaurus/plugin-content-docs';
import {ChevronRight} from 'lucide-react';

function getBreadcrumbHref(
  item: PropSidebarBreadcrumbsItem,
  isLast: boolean,
): string | undefined {
  // Current page — not a link
  if (isLast) {
    return undefined;
  }
  if (item.type === 'category') {
    // Prefer the category's own index link when listed; otherwise first child
    if (item.href && !item.linkUnlisted) {
      return item.href;
    }
    return findFirstSidebarItemLink(item);
  }
  return item.href;
}

function BreadcrumbsItemLink({
  children,
  href,
  isLast,
}: {
  children: ReactNode;
  href: string | undefined;
  isLast: boolean;
}): ReactNode {
  const className = 'breadcrumbs__link';
  if (isLast || !href) {
    return (
      <span className={className} {...(isLast ? {'aria-current': 'page'} : {})}>
        {children}
      </span>
    );
  }
  return (
    <Link className={className} href={href}>
      {children}
    </Link>
  );
}

function BreadcrumbsItem({
  children,
  active,
}: {
  children: ReactNode;
  active?: boolean;
}): ReactNode {
  return (
    <li
      className={clsx('breadcrumbs__item', {
        'breadcrumbs__item--active': active,
      })}>
      {children}
    </li>
  );
}

function BreadcrumbsSeparator(): ReactNode {
  return (
    <li className="breadcrumbs__separator" aria-hidden>
      <ChevronRight size={14} strokeWidth={2} />
    </li>
  );
}

export default function DocBreadcrumbs(): ReactNode {
  const breadcrumbs = useSidebarBreadcrumbs();
  const homePageRoute = useHomePageRoute();

  if (!breadcrumbs) {
    return null;
  }

  const items: ReactNode[] = [];

  if (homePageRoute) {
    items.push(<HomeBreadcrumbItem key="home" />);
  }

  breadcrumbs.forEach((item, idx) => {
    const isLast = idx === breadcrumbs.length - 1;
    const href = getBreadcrumbHref(item, isLast);

    if (items.length > 0) {
      items.push(<BreadcrumbsSeparator key={`sep-${idx}`} />);
    }

    items.push(
      <BreadcrumbsItem key={idx} active={isLast}>
        <BreadcrumbsItemLink href={href} isLast={isLast}>
          {item.label}
        </BreadcrumbsItemLink>
      </BreadcrumbsItem>,
    );
  });

  return (
    <>
      <DocBreadcrumbsStructuredData breadcrumbs={breadcrumbs} />
      <nav
        className={clsx(ThemeClassNames.docs.docBreadcrumbs, 'sc-doc-breadcrumbs')}
        aria-label={translate({
          id: 'theme.docs.breadcrumbs.navAriaLabel',
          message: 'Breadcrumbs',
          description: 'The ARIA label for the breadcrumbs',
        })}>
        <ol className="breadcrumbs">{items}</ol>
      </nav>
    </>
  );
}
