import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';

import Section from '../components/Section';
import allFeatures from '../components/features';
import YellowBanner from "../components/YellowBanner"
import BigHeader from "../components/BigHeader"

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Single-module projects" description="Page describing why Scala CLI is good for scripting with Scala.">
        <div className="container content">
			<YellowBanner image="img/fast-scala-cli.gif" title="Learn a language not a build tool">
				<p>Scala-cli is deigned in a way so you can focus on learning, not struggle with installation or build tool.</p>
			</YellowBanner>
		
			
			<BigHeader title="Education with Scala CLI" colsize="12" promptsign={true}></BigHeader>


			<Section className="section-image-box">
				{allFeatures().filter(f => f.props.projects)}
			</Section>

			
		</div>
    </Layout>
  );
};

export default Index;
