import React from 'react';

import ImageBox from '../components/ImageBox';

const featuresList = [
    <ImageBox image="versions.svg" title="Scala versions, dependencies and JVMs" 
      key="versions" projects="true"> 
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
      <p>
        <i>Some additional setup may be required for <a href="/install#scala-js">JS</a> and <a href="/install#scala-native">Native</a></i>
      </p>
    </ImageBox>,
    <ImageBox image="universal_tool.svg" title="Universal tool" key="universal" 
      projects="true">
      <p>
        If you want to use older <b>version of Scala</b> or
        run your code in <b>JS</b> or <b>Native</b> environments we've got you covered.
        <br/>
      </p>
      <p>Switching between platforms or Scala versions is as easy as changing a parameter.</p>
      <p> <i>Some additional setup may be required for <a href="/install#scala-js">JS</a> and <a href="/install#scala-native">Native</a></i></p>
    </ImageBox>,
    <ImageBox 
      image="buildtools.png" 
      title="We do not call Scala CLI a build tool" key="buildtool" projects="true">
        <p>
          Scala CLI shares some similarities with build tools,
          but doesn't aim at supporting multi-module projects,
          nor to be extended via a task system known from sbt, mill or bazel.
        </p>
        <p>
          Scala ecosystem has multiple amazing build tools, there is no need to create another multipurpose build tool.
        </p>
    </ImageBox>,
    <ImageBox 
        image="complete-install.svg" title="Complete installation"
        key="complete-install" education="true">
          <p>
            Scala CLI comes with batteries included. No additional installation is required, no more fluffing with setting up the correct Java version or <code>PATH</code>
          </p>
          <p>
            Scala CLI manages JVMs, Scala and other used tools under the hood.
          </p>
      </ImageBox>,
      <ImageBox 
       image="defaults.svg" title="Solid defaults" 
       key="defaults" education="true">
         <p>
           No additional configuration is needed to most Scala CLI commands.
         </p>
         <p>
           Scala CLI is configured out of the box to use the latest stable versions and other commands such as formatter or compiler contain reasonable defaults.
         </p>
     </ImageBox>,
      <ImageBox 
      image="learning_curve.svg" title="No learning curve" 
      key="curve" education="true">
        <p>
          Scala CLI does not use complex configuration language, its options are simple and self-explanatory
        </p>
        <p>
        There are no big differences in running repl or .scala files so expanding the results of repl session into a small project does not require learning new concepts from Scala CLI perspective
        </p>
    </ImageBox>,
    <ImageBox 
      image="powerful_scripts.svg" title="Scripts are as powerful as other programs" key="scripts-as-apps" scripting="true">
        <p>
          Scripts in Scala CLI can use dependencies and other features as standard Scala programs. Scala CLI is command-line first giving access to all its features without need for any configuration file or specific project structure.
        </p>
    </ImageBox>,
    <ImageBox 
      image="embbedable_scripts.svg" title="Embbedale Scripts" key="embed-scripts" scripting="true">
        <p>
          Scala CLI can be included in shebangs making your .scala or .sc files runnable
        </p>
        <p>
          Scala CLI support piping inputs in and is designed to be embeddable in other scripts turning Scala into proper scripting language
        </p>
    </ImageBox>,
    <ImageBox 
      image="fast-scripts.svg" title="Fast Scripts" key="fast-scripts" scripting="true">
        <p>
          Scala CLI provides multiple ways to reduce the biggest problem of JVM-based scripting solutions: slow start time. Scala CLI aggressively caches inputs removing need for recompilations.
        </p>
        <p>
          Scripts can be packaged into the native applications (using e.g. Scala Native) for even faster cold startups.
        </p>
    </ImageBox>,

    // Prototyping

    <ImageBox 
      image="self-contained-examples.svg" title="Self-contained examples" 
      key="self-contained-examples" prototyping="true">
      <p>
        With Scala CLI, configuration can be included in source code so complex examples can be self-contained and shipped as e.g. gist. Moreover, Scala CLI can compile, run and test gists without any manual work!
      </p>
      <p>
        Scala CLI is a perfect tool to submit and reproduce bugs
      </p>
    </ImageBox>
    
]

export default function allImageBoxs() {  
  return featuresList 
}
