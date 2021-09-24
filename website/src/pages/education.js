import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import TitleSection from '../components/TitleSection';
import Section from '../components/Section';
import allFeatures from '../components/features';

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Education" description="Page describing why scala-cli is good within educational purposes, mainly learning Scala.">
      <div className="container padding--sm content">
        <TitleSection><h1>scala-cli for Education</h1></TitleSection>

        <Section>
          <p>TODO: describe why scala-cli is a perfect for leatning Scala! Plus some image?</p>
        </Section>

        {allFeatures().filter(f => f.props.education)}
      </div>
    </Layout>
  );
};

export default Index;
