import type {ReactNode} from 'react';
import {cn} from '../lib/utils';

type SectionEyebrowProps = {
  children: ReactNode;
  className?: string;
  tone?: 'light' | 'dark' | 'brand';
};

/** Visdom / Sensinum-style section label — mono, tracked */
export function SectionEyebrow({
  children,
  className,
  tone = 'brand',
}: SectionEyebrowProps) {
  return (
    <p
      data-sc-eyebrow
      className={cn(
        'sc-eyebrow mb-5 font-mono text-[0.7rem] font-medium uppercase tracking-[0.18em]',
        tone === 'brand' && 'text-brand',
        tone === 'light' && 'text-brand-yellow',
        tone === 'dark' && 'text-slate-500 dark:text-slate-400',
        className,
      )}
    >
      {children}
    </p>
  );
}

type CtaProps = {
  href: string;
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'ghost' | 'yellow';
  className?: string;
};

export function Cta({href, children, variant = 'primary', className}: CtaProps) {
  return (
    <a
      href={href}
      className={cn(
        'sc-cta inline-flex items-center justify-center rounded-lg px-6 py-3 text-sm font-semibold tracking-[-0.01em] transition duration-interactive ease-interactive',
        variant === 'primary' && 'sc-cta--primary',
        variant === 'secondary' && 'sc-cta--secondary border',
        variant === 'ghost' &&
          'sc-cta--ghost border border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50 dark:border-slate-700 dark:bg-transparent dark:hover:bg-slate-800',
        variant === 'yellow' && 'sc-cta--yellow',
        className,
      )}
    >
      {children}
    </a>
  );
}
