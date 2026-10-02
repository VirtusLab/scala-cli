import type {ReactNode} from 'react';

type TitleSectionProps = {
  children?: ReactNode;
  right?: ReactNode;
};

export default function TitleSection({children, right}: TitleSectionProps) {
  return (
    <div className="row">
      <div className="install-section col col--6 col--offset-2 text--left">
        {children}
      </div>
      <div className="install-section col col--4 text--center">{right}</div>
    </div>
  );
}
