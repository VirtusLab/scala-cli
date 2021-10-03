import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import TitleSection from '../components/TitleSection';
import Section from '../components/Section';
import allFeatures from '../components/features';
import {HeaderSection, TitledSection} from '../components/Layouts'

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Scripting with Scala CLI" description="Page describing why Scala CLI is good for scripting with Scala.">
      <div className="container padding--sm content">
        
        <HeaderSection image="img/fast-scala-cli.gif">
          <h1>Scripting using all powers of Scala ecosystem</h1>
          {/* TODO: better text */}
          <p>Scala-cli allows you to use Scala to create and enhance scripts with using all the goodies of Scala.</p>
        
          <p>Use dependencies, declare tests or even package your scripts into native applications!</p>
        </HeaderSection>
      
        <h1>Scripting with Scala CLI</h1>


        {allFeatures().filter(f => f.props.scripting)}
      </div>
    </Layout>
  );
};

export default Index;
