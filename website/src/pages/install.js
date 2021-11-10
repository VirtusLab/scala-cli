import React from 'react';
import Layout from '@theme/Layout';

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

			<AdvancedInstallation/>

		</Section>
		</div>
    </Layout>
  );
};

export default Index;
