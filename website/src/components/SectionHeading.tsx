import type {ReactNode} from 'react';
import {cn} from '../lib/utils';

type SectionHeadingProps = {
  /** Plain string title, or rich nodes (e.g. joined titles) */
  title?: ReactNode;
  children?: ReactNode;
  as?: 'h1' | 'h2';
  promptsign?: boolean;
  light?: boolean;
  className?: string;
  id?: string;
};

/** Canonical product section title — Use Cases / BigHeader style */
export default function SectionHeading({
  title,
  children,
  as = 'h1',
  promptsign = false,
  light = false,
  className,
  id,
}: SectionHeadingProps) {
  const Tag = as;
  const content = children ?? title;
  return (
    <Tag
      id={id}
      className={cn(
        'sc-display sc-section-title max-w-3xl',
        light ? 'text-white' : 'text-ink dark:text-white',
        promptsign && 'sc-prompt',
        className,
      )}
    >
      {content}
    </Tag>
  );
}
