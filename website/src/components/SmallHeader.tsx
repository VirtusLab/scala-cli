import type {ReactNode} from 'react';
import {SectionEyebrow} from './Cta';
import SectionHeading from './SectionHeading';
import {cn} from '../lib/utils';

type SmallHeaderProps = {
  title: string;
  children?: ReactNode;
  promptsign?: boolean;
  className?: string;
};

export default function SmallHeader({
  title,
  children,
  promptsign = false,
  className,
}: SmallHeaderProps) {
  return (
    <div className={cn('mb-12 max-w-3xl md:mb-16', className)}>
      <SectionEyebrow>Features</SectionEyebrow>
      <SectionHeading
        className="sc-features-title"
        title={title}
        as="h2"
        promptsign={promptsign}
      />
      {children ? (
        <div className="mt-5 font-sans text-lg leading-8 text-fg-muted dark:text-slate-300">
          {children}
        </div>
      ) : null}
    </div>
  );
}
