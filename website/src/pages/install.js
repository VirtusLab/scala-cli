import React from 'react';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

import Section from "../components/Section"
import BigHeader from "../components/BigHeader"
import BasicInstall from "../components/BasicInstall"
import AdvancedInstallation from '../../docs/_advanced_install.mdx'

const Index = (props) => {
  return (
    <Layout title='Install Scala CLI' description="How to install Scala CLI">

      <div className="container content">

		{/* Install Scala CLI */}
        <Section className="section-install-cli">
			<div className="row">

				<BigHeader title="Quick start" colsize="4" promptsign={true}></BigHeader>

				<div className="col col--8">
					<BasicInstall/>
				</div>


			</div>
        </Section>
		
		

		<Section className="section-about advanced-install">
			{/* Anchor targets for deep linking - using Heading so Docusaurus can detect them */}
			<Heading as="h2" id="advanced-installation" style={{display: 'none', margin: 0, padding: 0}}>Advanced Installation</Heading>
			<Heading as="h2" id="scala-js" style={{display: 'none', margin: 0, padding: 0}}>Scala.js</Heading>
			<Heading as="h2" id="scala-native" style={{display: 'none', margin: 0, padding: 0}}>Scala Native</Heading>
			<AdvancedInstallation/>

		</Section>
		</div>
    </Layout>
  );
};

export default Index;
