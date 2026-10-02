import type {ReactNode} from 'react';
import Section from './Section';
import ThemedImage from '@theme/ThemedImage';
import {Cta, SectionEyebrow} from './Cta';
import {cn} from '../lib/utils';

type YellowBannerProps = {
  title: string;
  /** Console / GIF on the right. Omit for text-only hero (Use Cases). */
  image?: string;
  children?: ReactNode;
  showCtas?: boolean;
  eyebrow?: string;
};

export default function YellowBanner({
  title,
  image,
  children,
  showCtas = false,
  eyebrow = 'Scala CLI',
}: YellowBannerProps) {
  const hasMedia = Boolean(image);

  return (
    <Section band className="sc-hero-glow relative overflow-hidden">
      <div className="sc-dot-grid pointer-events-none absolute inset-0" aria-hidden />
      <div
        className={cn(
          'relative sc-section-y',
          hasMedia &&
            'grid items-center gap-12 md:grid-cols-2 md:gap-16 lg:gap-20',
        )}
      >
        <div className={cn(hasMedia ? 'max-w-xl' : 'max-w-3xl')}>
          <SectionEyebrow tone="light">{eyebrow}</SectionEyebrow>
          <h1 className="sc-hero-title sc-display sc-display-tight text-[2.35rem] leading-[1.08] sm:text-[3rem] lg:text-[3.75rem] lg:leading-[1.02]">
            {title}
          </h1>
          {children ? (
            <div className="sc-hero-copy mt-7 max-w-2xl space-y-4 font-sans text-[1.05rem] font-normal leading-8 md:text-lg md:leading-8">
              {children}
            </div>
          ) : null}
          {showCtas ? (
            <div className="mt-9 space-y-7">
              <div className="flex flex-wrap items-center gap-3">
                <Cta href="/install" variant="primary">
                  &gt;_ Install
                </Cta>
                <Cta href="/docs/overview" variant="secondary">
                  Documentation
                </Cta>
              </div>
              <a
                className="sc-hero-vl group inline-flex items-center gap-3 no-underline"
                href="https://virtuslab.com/"
                target="_blank"
                rel="noopener noreferrer"
              >
                <span className="font-mono text-[0.7rem] font-medium tracking-[0.1em] text-white/50 uppercase transition-colors group-hover:text-white/75">
                  Developed &amp; Maintained by
                </span>
                <img
                  className="sc-hero-vl-logo h-7 w-auto opacity-90 transition-opacity group-hover:opacity-100"
                  src="/img/virtuslab-logo.svg"
                  alt="VirtusLab"
                  width={110}
                  height={30}
                />
              </a>
            </div>
          ) : null}
        </div>

        {hasMedia ? (
          <div className="relative">
            <div
              className="pointer-events-none absolute -inset-8 rounded-2xl bg-[radial-gradient(circle_at_center,rgba(242,49,53,0.35),transparent_65%)] blur-2xl"
              aria-hidden
            />
            <div className="relative overflow-hidden rounded-lg border border-white/12 bg-ink-soft/90 p-2 shadow-[0_40px_80px_rgba(0,0,0,0.5)] backdrop-blur leading-none">
              <div className="mb-2 flex items-center gap-1.5 px-1 pt-0.5">
                <span className="h-2.5 w-2.5 rounded-full bg-brand" />
                <span className="h-2.5 w-2.5 rounded-full bg-brand-yellow" />
                <span className="h-2.5 w-2.5 rounded-full bg-white/30" />
                <span className="ml-3 font-mono text-[10px] tracking-[0.12em] text-slate-500 uppercase">
                  scala-cli
                </span>
              </div>
              <ThemedImage
                className="block w-full rounded-md"
                alt={image!}
                sources={{
                  light: `/img/${image}`,
                  dark: `/img/dark/${image}`,
                }}
              />
            </div>
          </div>
        ) : null}
      </div>
    </Section>
  );
}
