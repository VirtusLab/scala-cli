import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';

import Section from '../components/Section';
import SmallHeader from '../components/SmallHeader';
import ImageBox from '../components/ImageBox';

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Scripting" description="Page describing why Scala CLI is good for prototyping / experimenting / reproducing  Scala.">
		<div className="container content" style={{marginTop: "80px"}}>

			<SmallHeader title="prototyping / experimenting / reproducing with Scala CLI">
				<p>TODO: describe why Scala CLI is a perfect for scripting Plus some image?</p>
			</SmallHeader>

			<Section className="section-image-box">

				<ImageBox title="Scala versions, dependencies and JVMs" image="img/envs.gif" imgpos="right">
					<p>
						Scala CLI is built on top of coursier. This allows us to manage Scala versions, dependencies and JVMs so you can test your code in different environments by changing single option.
					</p>
					<p>
						Scala CLI ships with all its dependencies. No need to fluff with installing JVM or setting up PATH.
					</p>
				</ImageBox>

				<ImageBox title="Universal tool" image="img/envs.gif" imgpos="left">
					<p>
						If you want to use older version of Scala or run your code in JS or Native environments we've got you covered.
					</p>
					<p>
						<em>some additional setup may be required for JS and Native</em>
					</p>
				</ImageBox>

				<ImageBox title="We do not call Scala CLI a build tool" image="img/loga.png" imgpos="right">
					<p>
						Scala CLI shares some similarities with build tools, but doesn't aim at supporting multi-module projects, nor to be extended via a task system known from sbt, mill or bazel.
					</p>
					<p>
						Scala ecosystem has multiple amazing build tools, there is no need to create another one.
					</p>
				</ImageBox>

			</Section>

      </div>
    </Layout>
  );
};

export default Index;
