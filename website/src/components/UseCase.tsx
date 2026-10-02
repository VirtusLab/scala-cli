import type {ReactNode} from 'react';
import Layout from '@theme/Layout';
import Section from './Section';
import YellowBanner from './YellowBanner';
import FeaturesSection from './FeaturesSection';
import BigHeader from './BigHeader';
import type {UseCaseId} from './features';

type UseCaseProps = {
  title: string;
  description: string;
  headline: string;
  /** @deprecated unused — text-only hero on use-case pages */
  image?: string;
  id: UseCaseId;
  children?: ReactNode;
};

export default function UseCase({
  title,
  description,
  headline,
  id,
  children,
}: UseCaseProps) {
  return (
    <Layout title={title} description={description} key={title}>
      <div className="sc-page content">
        <YellowBanner title={headline} eyebrow="Use case">
          {children}
        </YellowBanner>

        <Section band className="sc-band-plain bg-white dark:bg-ink">
          <div className="sc-section-y">
            <BigHeader
              title={title}
              promptsign
              eyebrow="Use case"
              className="mb-10"
            />
            <FeaturesSection filterId={id} hideHeading showNav />
          </div>
        </Section>
      </div>
    </Layout>
  );
}
