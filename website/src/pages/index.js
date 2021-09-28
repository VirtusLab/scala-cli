import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import BasicInstall from "../components/basicInstall"
import allFeatures from "../components/features"
import TitleSection from "../components/TitleSection"
import Section from "../components/Section"

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title={siteConfig.title} description={siteConfig.tagline}>
      <div className="container padding--sm content">
        
        {/* HEADLINE */}
        <TitleSection>
          <h1>scala-cli</h1>
          <i>/skala-kli/</i>
        </TitleSection>
        <Section image="img/lift_off.png">
          <p>
            Scala CLI is comamnd-line tool to interact with Scala language.
            <br/>
            It allows to compile, run, test, package (and more) your Scala code. 
          </p>

          <p> Why Scala CLI?
            Scala CLI combines all the features you need to learn and use Scala in your (single-module) projects.
            Scala CLI is intuitive <code>and interactive</code>.
          </p>
          <p>
            If you are bored with reading documentation or landing pages you can just <a href="docs/installation">install</a> and <a href="docs/input">enjoy `scala-cli`</a>:
          </p>

          <p>
            Still here?
            <br/>
            Ok, let us convince you why Scala CLI is worth to give it a go.
          </p>


          <code>features marked as code are still under development</code>
        </Section>

        {/* Common features */}
        <div className="container text--left row common_features justify-content-start">
          <div className="col col--3 col--offset-1 red_border padding--md">
            <h2>Intuitive, simple</h2>

            <p>No complicated mechanism, tasks, plugins or extension. Single-module only </p>

            <p>
            All our commands have multiple aliases and follow well-known conventions.
            </p>
          </div>

          <div className="col col--3 red_border padding--md">
            <h2>Fast</h2>

            <p>Scala CLI is optimize to respond as quickly as possible.</p>

            <p>CLI is compiled to native code and compilations are offloaded to <a href="https://scalacenter.github.io/bloop/">bloop</a></p>
          </div>

          <div className="col col--3 red_border padding--md">
              <h2>Command-line first</h2>

              <p>Scala CLI does not require any configuration file and all in-file configuration can be overridden by command-line.</p>

              <p>No additional installation or setup of environment (like specific working directory) required.</p>
          </div>
        </div>


        <TitleSection>
          <div className="padding--lg">
            <h1>Designed for:</h1>
          </div>
        </TitleSection>

        <div className="row">
          <div className="col col--6 col--offset-1">
            <div className="padding--md"/>
            <div className="green_border">
              <img src="img/features.gif" />
            </div>
          </div>
          <div className="col col--5 text--left ">
              

              <p><a href="/education">education</a>
              <br/>
              Scala CLI is a help not a distraction while learning Scala, a library or programming in general <a href="/education">(read more)</a>.
              
              </p>
              

              <p>
                <a href="/scripting">scripting</a>
                <br/>
                Scala CLI has all tools to create or be integrated into scripts with whole power of Scala ecosystem <a href="/scripting"> (read more)</a>.
                
              </p>
              
              <p>
                <a href="/prototyping">prototyping, experimenting, reproducing</a>
                <br/>
                With Scala CLI, experimenting with different libraries, Scala or JVM versions or compiler options just easy 
                and fun <a href="/prototyping">(read more)</a>
              </p>

              <p>
                <a href="/projects">single-module projects</a>
                <br/>
                Scala CLI provides all tools to manage single-module project like CLI or basic web applications or sever-less lambdas. <a href="/projects">(read more)</a>.
              </p>

              <p>
                [your use case]
                <br/>
                If you see other use case for Scala CLI let us know using github discussions!
              </p>
              
          </div>
        </div>

        
        {/* Install */}
        <TitleSection>
          <div className="padding-lg">
            <h1 >Install Scala CLI</h1>
          </div>
        </TitleSection>

        <div className="row">
          <div className="install-section col col--7 col--offset-1 text--left">
            <BasicInstall/>
          </div>
          <div className="col col-4"/>
        </div>  



      <TitleSection><h1>Still undecided?<br/></h1>Here come our main features:</TitleSection>

      <div className="padding--md"/>


      {allFeatures()}
      </div>
    </Layout>
  );
};

export default Index;
