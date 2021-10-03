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
    </Feature>,
    <Feature 
      image="img/todo.svg" 
      title="Solid defaults" key="defaults" education="true">
        <p>
          No additional configuration is needed to most scala-cli commands.
        </p>,
        <p>
          Scala-cli is configured out of the box to use the latest stable version of Scala. Other commands such as formatter or compiler contain reasonable defaults.
        </p>
    </Feature>,
    <Feature 
        image="img/todo.svg" 
        title="Complete installation" key="complete-install" education="true">
          <p>
            Scala-cli comes with batteries included. No additional installation is required, no more fluffing with setting up the correct Java version or setting up <code>PATH</code>
          </p>,
          <p>
            Scala-cli manages JVMs, Scala and other used tools under the hood.
          </p>
      </Feature>,
      <Feature 
      image="img/todo.svg" title="No learning curve" key="curve" education="true">
        <p>
          Scala-cli does not use complex configuration language, its options are simple and self-explaining
        </p>,
        <p>
        There are no big differences in running repl or .scala files so expanding the results of repl session into a small project does not require learning new concepts from scala-cli perspective
        </p>
    </Feature>,
    <Feature 
      image="img/todo.svg" title="Scripts are as powerful as other programs" key="scripts-as-apps" scripting="true">
        <p>
          Scripts in scala-cli can use dependencies and other features as standard Scala programs. Scala-cli is command-line first giving access to all its feature without single build or configuration features.
        </p>
    </Feature>,
    <Feature 
      image="img/todo.svg" title="Embbedale Scripts" key="embed-scripts" scripting="true">
        <p>
          Scala-cli can be included in shebangs making scripts runnable (and that include dependencies and other goodies).
        </p>
        <p>
          Scala-cli support piping inputs and is designed to be embedded in other scripts opening Scala to any script specific use cases
        </p>
    </Feature>,
    <Feature 
      image="img/todo.svg" title="Fast Scripts" key="fast-scripts" scripting="true">
        <p>
          Scala-cli provides multiple ways to reduce the biggest problem of JVM-based scripting solutions: slow start time. Scala-cli aggressively caches inputs removing need for recompilations and support packing into the native applications (using e.g. Scala Native) for faster cold startups.
        </p>
    </Feature>,
    <Feature 
      image="img/todo.svg" title="Support for .sc files" key="sc-files-support" scripting="true">
        <p>
          Scala-cli is backwards compatible with ammonite scripts.
        </p>
        <p>
          No need to migrate your existing scripts to use all the powers of Scala-cli.
        </p>
    </Feature>,
    <Feature 
    image="img/todo.svg" title="Global defaults" key="sc-files-support" scripting="true">
      <p>
        Scala-cli is backwards compatible with ammonite scripts.
      </p>
      <p>
        No need to migrate your existing scripts to use all the powers of Scala-cli.
      </p>
    </Feature>,
    <Feature 
      image="img/todo.svg" title="Self-contained examples" key="self-contained-examples" prototyping="true">
      <p>
        With scala-cli, configuration can be included in source code so complex examples can be self-contained and shipped as e.g. gist. 
      </p>
      <p>
        Moreover, scala-cli can compile, run and test gists without any manual work!
      </p>
    </Feature>,
    
]

export default function allFeatures() {  
  return FeatureList 
}
