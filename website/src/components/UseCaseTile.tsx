import type {ReactNode} from 'react';
import type {LucideIcon} from 'lucide-react';
import {
  ArrowRight,
  FileCode2,
  FlaskConical,
  GraduationCap,
  MessageSquarePlus,
  Package,
} from 'lucide-react';
import {cn} from '../lib/utils';

type UseCaseTileProps = {
  title: string;
  description: ReactNode;
  slug?: string | false;
};

const useCaseIcons: Record<string, LucideIcon> = {
  education: GraduationCap,
  scripting: FileCode2,
  prototyping: FlaskConical,
  projects: Package,
};

export default function UseCaseTile({title, description, slug}: UseCaseTileProps) {
  const isLink = Boolean(slug);
  const Icon =
    isLink && typeof slug === 'string'
      ? useCaseIcons[slug]
      : MessageSquarePlus;

  const body = (
    <div
      className={cn(
        'relative flex h-full flex-col overflow-hidden rounded-xl border border-slate-200/80 bg-white p-7 md:p-8 dark:border-white/10 dark:bg-ink-soft',
        isLink &&
          'transition duration-interactive ease-interactive hover:-translate-y-1 hover:border-brand/35 hover:shadow-[0_28px_56px_rgba(15,23,42,0.1)] dark:hover:border-brand-yellow/35',
        !isLink && 'border-dashed bg-slate-50/80 dark:bg-slate-900/40 dark:border-white/12',
      )}
    >
      {Icon ? (
        <div
          className={cn(
            'mb-5 inline-flex h-10 w-10 items-center justify-center rounded-lg ring-1 ring-inset',
            isLink
              ? 'bg-brand/[0.07] text-brand ring-brand/12 transition duration-interactive ease-interactive group-hover:bg-brand/10 group-hover:ring-brand/20 dark:bg-brand-yellow/10 dark:text-brand-yellow dark:ring-brand-yellow/15'
              : 'bg-slate-100 text-slate-500 ring-slate-200/80 dark:bg-white/5 dark:text-slate-400 dark:ring-white/10',
          )}
          aria-hidden
        >
          <Icon size={20} strokeWidth={1.75} />
        </div>
      ) : null}

      <h3 className="sc-use-case-tile__title mb-3 font-sans text-xl font-semibold tracking-[-0.02em] text-ink dark:text-white">
        {title}
      </h3>

      <p className="sc-use-case-tile__desc mb-8 flex-1 font-sans text-[0.95rem] leading-7 text-fg-muted dark:text-slate-300">
        {description}
      </p>

      {isLink ? (
        <div className="mt-auto flex items-center gap-2 font-mono text-sm font-medium text-brand dark:text-brand-yellow">
          <span aria-hidden>&gt;_</span>
          Read more
          <ArrowRight
            size={14}
            strokeWidth={2.25}
            aria-hidden
            className="translate-x-0 transition duration-interactive ease-interactive group-hover:translate-x-1"
          />
        </div>
      ) : null}
    </div>
  );

  if (isLink && typeof slug === 'string') {
    return (
      <a
        href={`/${slug}`}
        className="sc-use-case-tile group block h-full"
      >
        {body}
      </a>
    );
  }

  return <div className="h-full">{body}</div>;
}
