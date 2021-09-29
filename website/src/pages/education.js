import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import TitleSection from '../components/TitleSection';
import Section from '../components/Section';
import allFeatures from '../components/features';

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Education" description="Page describing why Scala CLI is good within educational purposes, mainly learning Scala.">
      <div className="container padding--sm content">
        <TitleSection><h1>Scala CLI for Education</h1></TitleSection>

        <Section>
          <p>TODO: describe why Scala CLI is a perfect for learning Scala! Plus some image?</p>
        </Section>

        {allFeatures().filter(f => f.props.education)}
      </div>
    </Layout>
  );
};

export default Index;
