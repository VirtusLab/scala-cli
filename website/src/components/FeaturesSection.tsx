import type {ReactNode} from 'react';
import SmallHeader from './SmallHeader';
import ImageBox from './ImageBox';
import FeaturesStickyNav from './FeaturesStickyNav';
import {getFeatures, type UseCaseId} from './features';

type FeaturesSectionProps = {
  filterId?: UseCaseId;
  showNav?: boolean;
  heading?: string;
  intro?: ReactNode;
  /** Hide SmallHeader (when parent already rendered a heading) */
  hideHeading?: boolean;
};

export default function FeaturesSection({
  filterId,
  showNav = true,
  heading = 'Still undecided?',
  intro,
  hideHeading = false,
}: FeaturesSectionProps) {
  const features = getFeatures(filterId);
  const defaultIntro = (
    <>
      Here come our{' '}
      <span className="font-semibold text-ink dark:text-white">main features</span>
    </>
  );

  return (
    <div className="sc-features-section">
      {!hideHeading ? (
        <SmallHeader className="sc-features-heading" title={heading}>
          {intro ?? defaultIntro}
        </SmallHeader>
      ) : null}

      {showNav && features.length > 1 ? (
        <FeaturesStickyNav
          items={features.map(({id, navLabel}) => ({id, navLabel}))}
        />
      ) : null}

      <div className="sc-features-list">
        {features.map(f => (
          <ImageBox
            key={f.id}
            id={f.id}
            image={f.image}
            title={f.title}
            education={f.education}
            scripting={f.scripting}
            prototyping={f.prototyping}
            projects={f.projects}
          >
            {f.body}
          </ImageBox>
        ))}
      </div>
    </div>
  );
}
