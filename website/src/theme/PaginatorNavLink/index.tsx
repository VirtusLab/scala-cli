import React, {type ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import {ChevronLeft, ChevronRight} from 'lucide-react';
import type {Props} from '@theme/PaginatorNavLink';

export default function PaginatorNavLink(props: Props): ReactNode {
  const {permalink, title, subLabel, isNext} = props;
  return (
    <Link
      className={clsx(
        'pagination-nav__link',
        isNext ? 'pagination-nav__link--next' : 'pagination-nav__link--prev',
      )}
      to={permalink}
    >
      {subLabel && <div className="pagination-nav__sublabel">{subLabel}</div>}
      <div className="pagination-nav__label">
        {!isNext && (
          <ChevronLeft
            className="pagination-nav__chevron"
            size={18}
            strokeWidth={2.25}
            aria-hidden
          />
        )}
        <span className="pagination-nav__title">{title}</span>
        {isNext && (
          <ChevronRight
            className="pagination-nav__chevron"
            size={18}
            strokeWidth={2.25}
            aria-hidden
          />
        )}
      </div>
    </Link>
  );
}
