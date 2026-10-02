import type {ReactNode} from 'react';
import Section from './Section';
import {cn} from '../lib/utils';

type InstallBandProps = {
  children: ReactNode;
  /** Alternating surface — matches Home plain/soft rhythm */
  tone?: 'plain' | 'soft';
  className?: string;
};

/** Full-bleed band for one (or a few) install `SectionAbout` blocks. */
export default function InstallBand({
  children,
  tone = 'plain',
  className,
}: InstallBandProps) {
  return (
    <Section
      band
      className={cn(
        tone === 'soft'
          ? 'sc-band-soft bg-surface dark:bg-ink-soft'
          : 'sc-band-plain bg-white dark:bg-ink',
        className,
      )}
    >
      <div className="sc-section-y advanced-install-band">{children}</div>
    </Section>
  );
}
