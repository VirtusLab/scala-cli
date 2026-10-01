import type {ReactNode} from 'react';
import type {LucideIcon} from 'lucide-react';

type IconBoxProps = {
  title: string;
  icon?: LucideIcon;
  children?: ReactNode;
};

/** Benefit card with a compact Lucide mark (static — not a link). */
export default function IconBox({title, icon: Icon, children}: IconBoxProps) {
  return (
    <div className="relative flex h-full flex-col overflow-hidden rounded-xl border border-slate-200/80 bg-white p-7 md:p-8 dark:border-white/10 dark:bg-ink-soft">
      {Icon ? (
        <div
          className="mb-5 inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand/[0.07] text-brand ring-1 ring-inset ring-brand/12 dark:bg-brand-yellow/10 dark:text-brand-yellow dark:ring-brand-yellow/15"
          aria-hidden
        >
          <Icon size={20} strokeWidth={1.75} />
        </div>
      ) : null}
      <h3 className="mb-3 font-sans text-xl font-semibold tracking-[-0.02em] text-ink dark:text-white">
        {title}
      </h3>
      <div className="sc-prose text-[0.95rem] leading-7 text-fg-muted dark:text-slate-300">
        {children}
      </div>
    </div>
  );
}
