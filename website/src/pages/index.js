import React from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';



const Features = (props) => {
  const features = [
    {
      title: "Learn a language, not a build tool",
      content:
      <p>
        <p>
          Scala-cli combines all the features you need to learn and use Scala in your (simple) projects.
          Scala-cli is intuitive <code>and interactive</code>.
          We believe that you can just <a href="docs/installation">install</a> and <a href="docs/input">run scala-cli</a> for
          the first time and just skip reading rest of this page.
        </p>
        <p>
          Are you still with us?
          <br/>
          Ok, let us convince you why scala-cli is worth to give it a go.
        </p>

        <code>features marked as code are still under development</code>
      </p>,
      image: "img/lift_off.png",
      imageAlign: "left",
    },
    {
      title: "Easy to install",
      content:
        <p>
          <p>
            Our installation instructions are as simple as possible.
          </p>
          Just pick your <a href="https://github.com/VirtuslabRnD/scala-cli/releases/tag/nightly">distribution</a> and <a href="docs/installation">install</a> scala-cli.
          <br/>
          No need to fluff with installing JVM or setting up PATH.
        </p>,
      // TODO distribution dropdown not this lame screenshoot
      image: "img/installation.png",
      imageAlign: "right",
    },
    {
      title: "Responsive and fast",
      content:
        "Scala-cli is designed to be as fast and responsive as possible. One of our goals is to prove that Scala does not necessary needs ages to compile and run.",
      image:
        "img/fast-scala-cli.gif",
      imageAlign: "left",
    },
    {
      title: "All the features that you need",
      content:
        <p>
        Scala-cli can <b>run</b>, <b>compile</b> and <code>format</code> your code.
        We support <b>REPL</b> and <b>scripts</b>.
        We can run your <b>tests</b> or even <b>package</b> your project so you can share results of your work!",
        </p>,
      image: "img/features.gif",
      imageAlign: "right",
    },
    {
      title: "Universal tool",
      content:
        <p>
          If you want to use older <b>version of Scala</b> or
          run your code in <b>JS</b> or <b>Native</b> environments we've got you covered.
          <br/>
          <i>some additional <a href="TODO?">setup</a> may be required for JS and Native</i>
        </p>,
      image:
        "img/envs.gif",
      imageAlign: "left",
    },
    {
      title: "Scala-cli is NOT a build tool",
      content: [
        <p>
          Scala-cli shares some similarities with build tools,
          but doesn't aim at supporting multi-module projects,
          nor to be extended via a task system.
        </p>,
        <p>
          Scala ecosystem has multiple amazing build tools, there is no need to create another one.
        </p>
      ],
      image: "https://user-images.githubusercontent.com/1408093/68486864-dd9f2b00-01f6-11ea-9291-d3a7ce6ef225.png",
      imageAlign: "right",
    },
    {
      title: "Single module, basics commands",
      content:
        <p>
          Scala-cli is designed to fill the roles when complexity of a proper build tool kills productivity.
          <br/>
          Scala-cli supports single module that consists of main sources and <code>tests</code>.
          We have only limited set of commands and do not plan for any extension mechanism.
        </p>,

      image: "img/commands.png",
      imageAlign: "left",
    },
    {
      title: "Can scala-cli becomes just scala?",
      content:
        <p>
          <p>
            We hope that scala-cli could one day be the default `scala` command. For now scala-cli is just a bit more than a working prototype and we need your feedback: tell us what you think about the idea, features and ergonomics of scala-cli.
          </p>
          <p>
            Should scala-cli also target simple libraries, so that we should provide support for cross compiling?
          </p>
          <p>
            Do you think that having a `scala-compose` or similar tool that will allow to combine multiple scala-cli projects is a good idea?
          </p>
          <p>Let us know!</p>
        </p>,

      image: "img/scala-spiral.png",
      imageAlign: "right",
    }
  ];
  return (
    <div>
      {features.map((feature) => (
        <div className="hero text--center" key={feature.title}>
          <div className={`container ${feature.imageAlign === 'right' ? 'flex-row' : 'flex-row-reverse'}`}>
            <div className="padding--md">
              <h2 className="hero__subtitle">{feature.title}</h2>
              <p>{feature.content}</p>
            </div>
            <div className="padding-vert--md img-holder">
              <img src={feature.image} />
            </div>
          </div>
        </div>
      ))}
    </div >
  );
};

const Index = (props) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title={siteConfig.title} description={siteConfig.tagline}>
      <div className="hero text--center">
        <div className="container ">
          <div className="padding-vert--md">
            <h1 className="hero__title">{siteConfig.title}</h1>


            <Features />

          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Index;
