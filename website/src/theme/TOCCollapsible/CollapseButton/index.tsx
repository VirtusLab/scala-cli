import type {ReactNode} from 'react';
import clsx from 'clsx';
import Translate from '@docusaurus/Translate';
import {ChevronDown, List} from 'lucide-react';
import type {Props} from '@theme/TOCCollapsible/CollapseButton';

/**
 * Mobile "On this page" disclosure — Lucide list + chevron, Metronic chrome.
 */
export default function TOCCollapsibleCollapseButton({
  collapsed,
  ...props
}: Props): ReactNode {
  return (
    <button
      type="button"
      {...props}
      className={clsx(
        'clean-btn',
        'sc-toc-mobile__button',
        !collapsed && 'sc-toc-mobile__button--expanded',
        props.className,
      )}
    >
      <span className="sc-toc-mobile__label">
        <List
          className="sc-toc-mobile__label-icon"
          size={16}
          strokeWidth={2.25}
          aria-hidden
        />
        <Translate
          id="theme.TOCCollapsible.toggleButtonLabel"
          description="The label used by the button on the collapsible TOC component"
        >
          On this page
        </Translate>
      </span>
      <ChevronDown
        className="sc-toc-mobile__chevron"
        size={16}
        strokeWidth={2.25}
        aria-hidden
      />
    </button>
  );
}
