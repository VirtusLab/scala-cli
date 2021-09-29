import React from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import BasicInstall from "../components/basicInstall"

const Feature = (props) =>
  <div className="row padding--md feature-row">
    <div className="col col--1"/>
    <div className="col col--6" key="img">
      {!props.image ? "" : <div className="green_border"><img src={props.image}/></div>}
    </div>
    <div className="col col--5" key="text">
      <div className="padding--md"/>
      <h3>{props.title}</h3>
      <div className="padding--sm"/>
      {props.children}
    </div>
    <div className="col col--1"/>
  </div>

const FeatureList = [
    <Feature image="img/envs.gif" title="Scala versions, dependencies and JVMs" 
      key="versions" education="true" projects="true" scripting="true" prototyping="true"> 
      <p>
        Scala CLI is built on top of coursier
        <br/>
        This allow us to manage Scala versions, dependencies and JVMs so you can test your code in different environments by changing single option.
      </p>

      <p>
        Scala CLI ships with all its dependencies
        <br/>
        No need to fluff with installing JVM or setting up PATH. 
      </p>
    </Feature>,
    <Feature image="img/envs.gif" title="Universal tool" key="universal" 
      projects="true" scripting="true" prototyping="true">
      <p>
        If you want to use older <b>version of Scala</b> or
        run your code in <b>JS</b> or <b>Native</b> environments we've got you covered.
        <br/>
        <i>some additional <a href="TODO?">setup</a> may be required for JS and Native</i>
      </p>
    </Feature>,
    <Feature 
      image="https://user-images.githubusercontent.com/1408093/68486864-dd9f2b00-01f6-11ea-9291-d3a7ce6ef225.png" 
      title="We do not call Scala CLI a build tool" key="buildtool" projects="true">
        <p>
          Scala CLI shares some similarities with build tools,
          but doesn't aim at supporting multi-module projects,
          nor to be extended via a task system known from sbt, mill or bazel.
        </p>,
        <p>
          Scala ecosystem has multiple amazing build tools, there is no need to create another one.
        </p>
   </Feature>
]

export default function allFeatures() {  
  return FeatureList 
}
