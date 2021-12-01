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
		<YellowBanner title="Scala CLI is a command-line tool to interact with the Scala language." image="gifs/demo.gif">
			<p>
				It lets you compile, run, test, and package your Scala code (and more!)
			</p>
		</YellowBanner>
			


		{/* About */}
	  	<Section className="section-about">

			<SectionAbout title="Why Scala CLI?">
				<p>
					Scala CLI combines <em>all</em> the features you need to learn and use Scala in your scripts, playgrounds and (single-module) projects.
				</p>
				<p>
					To get started you can read <a href="/docs/overview">the documentation</a>, or just <a href="/install">install</a> and enjoy <code>scala-cli</code>.
				</p>
			</SectionAbout>

		</Section>


		{/* Who is Scala CLI designed for? */}
		<Section className="section-features">



			<div className="section-features__row row">
				
				<IconBox title="Intuitive, simple" icon="img/hand.png">
					<strong>No complicated mechanisms, tasks, plugins or extensions:</strong> just a single-module. All our commands have multiple aliases and follow well-known conventions.
				</IconBox>

				<IconBox title="Fast" icon="img/rocket.png">
					<strong>Scala CLI is optimized to be as fast as possible.</strong> CLI is compiled to native code and compilations are <a href="/docs/reference/bloop">offloaded to bloop</a>.
				</IconBox>

				<IconBox title="Command-line first" icon="img/monitor.png">
					<strong>Scala CLI does not require a configuration file, and all in-file configurations can be overridden by command-line.</strong> No additional installation or setup of an environment (such as a specific working directory) are required.
				</IconBox>

			</div>
		
		</Section>



		<div id="use_cases"/>

		{/* Who is Scala CLI designed for? */}
		<Section className="section-use-tiles">
          <div className="row">

            <BigHeader title="Who is Scala CLI designed for?" colsize="12" promptsign={true}></BigHeader>

            <div className="col col--12">
              <div className="use-boxes row">

                <UseCaseTile title="Education"
                             slug="education"
                             description="Scala CLI is a help — not a distraction — while learning Scala, a library or programming in general.">
                </UseCaseTile>

                <UseCaseTile title="Scripting"
                             slug="scripting"
                             description="Scala CLI has all the tools to create (or be integrated into) scripts with the whole power of the Scala ecosystem.">
                </UseCaseTile>

                <UseCaseTile title="Prototyping, Experimenting, Reproducing"
                             slug="prototyping"
                             description="With Scala CLI, experimenting with different libraries, Scala or JVM versions, or compiler options is easy and fun.">
                </UseCaseTile>

                <UseCaseTile title="Single-module projects"
                             slug="projects"
                             description="Scala CLI provides all the tools you need to manage single-module projects like CLI or basic web applications, or server-less lambdas.">
                </UseCaseTile>

                <UseCaseTile title="Your use case"
                             slug={false}
                             description=
                             {<span>If you see other use cases for Scala CLI, let us know using <a href="https://github.com/VirtusLab/scala-cli/discussions/categories/ideas">GitHub Discussions!</a></span>}>
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
