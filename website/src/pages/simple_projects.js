import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import TitleSection from '../components/TitleSection';
import Section from '../components/Section';
import allFeatures from '../components/features';

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Simple projects" description="Page describing why scala-cli is good for scripting with Scala.">
      <div className="container padding--sm content">
        <TitleSection><h1>Managing simple projects with scala-cli</h1></TitleSection>

        <Section>
          <p>TODO: describe why scala-cli is a perfect for scripting Plus some image?</p>
        </Section>

        {allFeatures().filter(f => f.props.simple_projects)}
      </div>
    </Layout>
  );
};

export default Index;
