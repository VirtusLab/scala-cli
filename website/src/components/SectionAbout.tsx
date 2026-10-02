import type {ReactNode} from 'react';
import useBrokenLinks from '@docusaurus/useBrokenLinks';
import {SectionEyebrow} from './Cta';
import SectionHeading from './SectionHeading';
import {cn} from '../lib/utils';

export type SectionAboutLayout = 'split' | 'stack';
export type SectionAboutTitleJoin = 'slash' | 'amp' | '/' | '&';

type SectionAboutProps = {
  /** Primary title when `titles` is not set */
  title?: string;
  /**
   * Multiple title parts joined in the heading.
   * Array, or comma-separated string for easier MDX: `titles="Scala.js,Scala Native"`.
   */
  titles?: string[] | string;
  /** How to join `titles` — `/` (default) or `&` in muted accent */
  titleJoin?: SectionAboutTitleJoin;
  children?: ReactNode;
  /** @deprecated unused — kept for MDX call sites */
  colBigTitle?: number | string;
  /** Section label; pass `false` or `""` to hide */
  eyebrow?: string | false;
  promptsign?: boolean;
  /**
   * `split` — 1+2 on 3-col grid (Home Why).
   * `stack` — title on top, content full width (Install).
   */
  layout?: SectionAboutLayout;
  /**
   * With `layout="stack"`: hoist the first Tabs tablist into the header row
   * (OS / shell switcher sits to the right of the title).
   */
  headerTabs?: boolean;
  /** Explicit anchor id (defaults from title / titles) */
  id?: string;
  /**
   * Extra hash targets (DOM + broken-link registry).
   * Use for legacy deep links (e.g. `#scala-js` → JS/Native section).
   */
  anchorIds?: string[];
  /** Include this section in Install (or other) jump TOC */
  toc?: boolean;
  /** Short label for TOC; defaults to title / joined titles */
  navLabel?: string;
  className?: string;
};

function parseTitles(
  title: string | undefined,
  titles: string[] | string | undefined,
): string[] {
  if (titles == null) {
    return title ? [title] : [];
  }
  if (Array.isArray(titles)) {
    return titles.map(t => t.trim()).filter(Boolean);
  }
  return titles
    .split(',')
    .map(t => t.trim())
    .filter(Boolean);
}

function joinChar(join: SectionAboutTitleJoin): '/' | '&' {
  if (join === 'amp' || join === '&') return '&';
  return '/';
}

function slugify(parts: string[]): string {
  return parts
    .join('-')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

function JoinedHeading({
  parts,
  join,
}: {
  parts: string[];
  join: SectionAboutTitleJoin;
}) {
  const sep = joinChar(join);
  if (parts.length <= 1) {
    return <>{parts[0] ?? ''}</>;
  }
  return (
    <>
      {parts.map((part, i) => (
        <span key={`${part}-${i}`}>
          {i > 0 ? (
            <span className="sc-title-join" aria-hidden="true">
              {` ${sep} `}
            </span>
          ) : null}
          {part}
        </span>
      ))}
    </>
  );
}

export default function SectionAbout({
  title,
  titles,
  titleJoin = 'slash',
  children,
  eyebrow = 'Why',
  promptsign = true,
  layout = 'split',
  headerTabs = false,
  id: idProp,
  anchorIds = [],
  toc = false,
  navLabel,
  className,
}: SectionAboutProps) {
  const parts = parseTitles(title, titles);
  const id = idProp ?? slugify(parts);
  const hasBody = children != null && children !== false;
  const showEyebrow = Boolean(eyebrow);
  const isStack = layout === 'stack';
  const hoistTabs = isStack && headerTabs;
  const tocLabel =
    navLabel ??
    (parts.length > 1
      ? parts.join(` ${joinChar(titleJoin)} `)
      : (parts[0] ?? id));

  // Docusaurus only auto-registers anchors from @theme/Heading — product
  // sections use plain headings, so register explicitly for build checks.
  const brokenLinks = useBrokenLinks();
  brokenLinks.collectAnchor(id);
  for (const alias of anchorIds) {
    brokenLinks.collectAnchor(alias);
  }

  const heading = (
    <SectionHeading as="h2" promptsign={promptsign}>
      <JoinedHeading parts={parts} join={titleJoin} />
    </SectionHeading>
  );

  return (
    <div
      id={id}
      className={cn(
        'sc-about',
        isStack
          ? cn(
              'sc-about--stack flex flex-col gap-5 md:gap-6',
              hoistTabs && 'sc-about--header-tabs',
            )
          : 'grid gap-5 md:grid-cols-3 md:gap-6',
        className,
      )}
      {...(toc
        ? {'data-sc-toc': '', 'data-sc-toc-label': tocLabel}
        : {})}
    >
      {anchorIds.map(alias => (
        <span key={alias} id={alias} className="sr-only" aria-hidden />
      ))}
      <div className={cn('sc-about__head', isStack && 'min-w-0')}>
        {showEyebrow ? <SectionEyebrow>{eyebrow}</SectionEyebrow> : null}
        {heading}
      </div>
      {hasBody ? (
        <div
          className={cn(
            'sc-prose sc-about__body space-y-5 self-start font-sans text-[1.05rem] leading-8 text-fg-muted md:text-lg md:leading-8 dark:text-slate-300',
            !isStack && 'md:col-span-2',
            isStack && 'w-full min-w-0',
          )}
        >
          {children}
        </div>
      ) : null}
    </div>
  );
}
