import type {ReactNode} from 'react';

type HeaderSectionProps = {
  children?: ReactNode;
  image?: string;
};

export function HeaderSection({children, image}: HeaderSectionProps) {
  return (
    <div className="row headerSection padding--lg margin--lg">
      <div className="col col--6 text--left">{children}</div>
      <div className="col col--6">{image ? <img src={image} alt="" /> : null}</div>
    </div>
  );
}

type TitledSectionProps = {
  title: string;
  children?: ReactNode;
};

export function TitledSection({title, children}: TitledSectionProps) {
  return (
    <div className="row titledSection padding--lg margin--lg">
      <div className="col col--3">
        <h1>{title}</h1>
      </div>
      <div className="col col--9 text--left">{children}</div>
    </div>
  );
}
