import type {ReactNode} from 'react';
import useBrokenLinks from '@docusaurus/useBrokenLinks';
import {cn} from '../lib/utils';

type SectionProps = {
  children: ReactNode;
  className?: string;
  /** Full-bleed band background; inner content uses sc-frame */
  band?: boolean;
  id?: string;
};

export default function Section({children, className, band = false, id}: SectionProps) {
  const brokenLinks = useBrokenLinks();
  if (id) {
    brokenLinks.collectAnchor(id);
  }

  if (band) {
    return (
      <section id={id} className={cn('sc-bleed', className)}>
        <div className="sc-frame">{children}</div>
      </section>
    );
  }
  return (
    <section id={id} className={cn('w-full', className)}>
      {children}
    </section>
  );
}
