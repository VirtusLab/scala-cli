import {cn} from '../lib/utils';
import {SectionEyebrow} from './Cta';
import SectionHeading from './SectionHeading';

type BigHeaderProps = {
  title: string;
  colsize?: string | number;
  promptsign?: boolean;
  className?: string;
  eyebrow?: string;
  light?: boolean;
};

export default function BigHeader({
  title,
  promptsign = false,
  className,
  eyebrow,
  light = false,
}: BigHeaderProps) {
  return (
    <div className={cn('w-full', className)}>
      {eyebrow ? (
        <SectionEyebrow tone={light ? 'light' : 'brand'}>{eyebrow}</SectionEyebrow>
      ) : null}
      <SectionHeading title={title} promptsign={promptsign} light={light} />
    </div>
  );
}
