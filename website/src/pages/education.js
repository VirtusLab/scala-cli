import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import allFeatures from '../components/features';
import {HeaderSection, TitledSection} from '../components/Layouts'
import Section from '../components/Section';

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Education" description="Page describing why Scala CLI is good within educational purposes, mainly learning Scala.">
      <div className="container padding--sm content">
        <HeaderSection image="img/fast-scala-cli.gif">
          <h1>Learn a language not a build tool</h1>

          <p>Scala-cli is deigned in a way so you can focus on learning, not struggle with installation or build tool.</p>
        </HeaderSection>
      
        <h1>Education with Scala CLI</h1>

		<Section className="section-image-box">
			{allFeatures().filter(f => f.props.education)}
		</Section>
      </div>
    </Layout>
  );
};

export default Index;
