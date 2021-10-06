import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';

import Section from "../components/Section"
import ImageBox from "../components/ImageBox"
import SmallHeader from "../components/SmallHeader"
import IconBox from "../components/IconBox"
import SectionAbout from "../components/SectionAbout"
import UseCaseTile from "../components/UseCaseTile"
import BigHeader from "../components/BigHeader"
import BasicInstall from "../components/BasicInstall"
import YellowBanner from "../components/YellowBanner"
import allFeatures from '../components/features';


const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title={siteConfig.title} description={siteConfig.tagline}>

      <div className="container content">

		{/* Headline */}
		<YellowBanner title="Scala CLI is a command-line tool to interact with Scala language." image="demo.svg">
			<p>
				It allows to compile, run, test, package (and more) your Scala code.
			</p>
		</YellowBanner>
			


		{/* About */}
	  	<Section className="section-about">

			<SectionAbout title="Why Scala CLI?">
				<p>
					Scala CLI combines all the features you need to learn and use Scala in your (single-module) projects. Scala CLI is intuitive <span className="tooltip" data-tooltip="Hello World fdsafdas fdsfdsafd fdasfdasf">and interactive</span>
				</p>
				<p>
					If you are bored with reading documentation or landing pages you can just <a href="#install">install</a> and <a href="#enjoy">enjoy `scala-cli`</a>.
				</p>
			</SectionAbout>

		</Section>


		{/* Who is Scala CLI designed for? */}
		<Section className="section-features">

			<SmallHeader title="Caught your interest?">
				Let us convince you why Scala CLI is <span>worth to give it a go.</span>
			</SmallHeader>

			<div className="section-features__row row">
				
				<IconBox title="Intuitive, simple" icon="img/hand.png">
					<strong>No complicated mechanism, tasks, plugins or extension.</strong> Single-module only. All our commands have multiple aliases and follow well-known conventions.
				</IconBox>

				<IconBox title="Fast" icon="img/rocket.png">
					<strong>Scala CLI is optimize to respond as quickly as possible.</strong> CLI is compiled to native code and compilations are <a href="#href">offloaded to bloop</a>
				</IconBox>

				<IconBox title="Command-line first" icon="img/monitor.png">
					<strong>Scala CLI does not require any configuration file and all in-file configuration can be overridden by command-line.</strong> No additional installation or setup of environment (like specific working directory) required.
				</IconBox>

			</div>
		
		</Section>


		{/* Who is Scala CLI designed for? */}
		<Section className="section-use-tiles">
          <div className="row">

            <BigHeader title="Who is Scala CLI designed for?" colsize="12" promptsign={true}></BigHeader>

            <div className="col col--12">
              <div className="use-boxes row">

                <UseCaseTile title="Education"
                             slug="education"
                             description="Scala CLI is a help not a distraction while learning Scala, a library or programming in general.">
                </UseCaseTile>

                <UseCaseTile title="Scripting"
                             slug="scripting"
                             description="Scala CLI has all tools to create or be integrated into scripts with whole power of Scala ecosystem.">
                </UseCaseTile>

                <UseCaseTile title="Prototyping, Experimenting, Reproducing"
                             slug="prototyping"
                             description="With Scala CLI, experimenting with different libraries, Scala or JVM versions or compiler options just easy and fun.">
                </UseCaseTile>

                <UseCaseTile title="Single-module projects"
                             slug="projects"
                             description="Scala CLI provides all tools to manage single-module project like CLI or basic web applications or sever-less lambdas.">
                </UseCaseTile>

                <UseCaseTile title="/// your use case"
                             slug={false}
                             description="If you see other use case for Scala CLI let us know using github discussions!">
                </UseCaseTile>

              </div>
            </div>

          </div>
        </Section>


		{/* Install Scala CLI */}
        <Section className="section-install-cli">
			<div className="row">

				<BigHeader title="Install Scala CLI" colsize="4" promptsign={true}></BigHeader>

				<div className="col col--8">
					<BasicInstall/>
				</div>

			</div>
        </Section>


		{/* Still undecided? */}
		<Section className="section-image-box">

			<SmallHeader title="Still undecided?">
				Here come our <span>main features</span>
			</SmallHeader>
	
			
			{allFeatures()}
		</Section>
				
		</div>

    </Layout>
  );
};

export default Index;
