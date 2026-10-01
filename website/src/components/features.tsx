import type {ReactNode} from 'react';
import ImageBox from './ImageBox';

export type UseCaseId = 'education' | 'scripting' | 'prototyping' | 'projects';

export type FeatureItem = {
  id: string;
  navLabel: string;
  title: string;
  image: string;
  education?: boolean;
  scripting?: boolean;
  prototyping?: boolean;
  projects?: boolean;
  body: ReactNode;
};

export const featuresCatalog: FeatureItem[] = [
  {
    id: 'versions',
    navLabel: 'Versions & JVMs',
    title: 'Scala versions, dependencies and JVMs',
    image: 'gifs/versions.gif',
    projects: true,
    body: (
      <>
        <p>
          Scala CLI is built on top of coursier
          <br />
          This allow us to manage Scala versions, dependencies and JVMs so you
          can test your code in different environments by changing single
          option.
        </p>
        <p>
          Scala CLI ships with all its dependencies
          <br />
          No need to fluff with installing JVM or setting up PATH.
        </p>
        <p>
          <i>
            Some additional setup may be required for{' '}
            <a href="/install#scala-js">JS</a> and{' '}
            <a href="/install#scala-native">Native</a>
          </i>
        </p>
      </>
    ),
  },
  {
    id: 'universal',
    navLabel: 'Universal tool',
    title: 'Universal tool',
    image: 'gifs/universal_tool.gif',
    projects: true,
    body: (
      <>
        <p>
          If you want to use older <b>version of Scala</b> or run your code in{' '}
          <b>JS</b> or <b>Native</b> environments we&apos;ve got you covered.
          <br />
        </p>
        <p>
          Switching between platforms or Scala versions is as easy as changing a
          parameter.
        </p>
        <p>
          {' '}
          <i>
            Some additional setup may be required for{' '}
            <a href="/install#scala-js">JS</a> and{' '}
            <a href="/install#scala-native">Native</a>
          </i>
        </p>
      </>
    ),
  },
  {
    id: 'buildtool',
    navLabel: 'Not a build tool',
    title: 'We do not call Scala CLI a build tool',
    image: 'buildtools.png',
    projects: true,
    body: (
      <>
        <p>
          Scala CLI shares some similarities with build tools, but doesn&apos;t
          aim at supporting multi-module projects, nor to be extended via a task
          system known from sbt, mill or bazel.
        </p>
        <p>
          Scala ecosystem has multiple amazing build tools, there is no need to
          create another multipurpose build tool.
        </p>
      </>
    ),
  },
  {
    id: 'complete-install',
    navLabel: 'Complete install',
    title: 'Complete installation',
    image: 'gifs/complete-install.gif',
    education: true,
    body: (
      <>
        <p>
          Scala CLI comes with batteries included. No additional installation is
          required, no more fluffing with setting up the correct Java version or{' '}
          <code>PATH</code>
        </p>
        <p>
          Scala CLI manages JVMs, Scala and other used tools under the hood.
        </p>
      </>
    ),
  },
  {
    id: 'defaults',
    navLabel: 'Defaults',
    title: 'Solid defaults',
    image: 'gifs/defaults.gif',
    education: true,
    body: (
      <>
        <p>No additional configuration is needed to most Scala CLI commands.</p>
        <p>
          Scala CLI is configured out of the box to use the latest stable
          versions and other commands such as formatter or compiler contain
          reasonable defaults.
        </p>
      </>
    ),
  },
  {
    id: 'curve',
    navLabel: 'Learning curve',
    title: 'No learning curve',
    image: 'gifs/learning_curve.gif',
    education: true,
    body: (
      <>
        <p>
          Scala CLI does not use a complex configuration language, its options
          are simple and self-explanatory.
        </p>
        <p>
          There are no big differences in running repl or .scala files so
          expanding the results of repl session into a small project does not
          require learning new concepts from Scala CLI perspective
        </p>
      </>
    ),
  },
  {
    id: 'scripts-as-apps',
    navLabel: 'Powerful scripts',
    title: 'Scripts are as powerful as other programs',
    image: 'gifs/powerful_scripts.gif',
    scripting: true,
    body: (
      <p>
        Scripts in Scala CLI can use dependencies and other features as standard
        Scala programs. Scala CLI is command-line first, giving access to all
        its features without the need for any configuration files or specific
        project structure.
      </p>
    ),
  },
  {
    id: 'embed-scripts',
    navLabel: 'Embeddable',
    title: 'Embeddable Scripts',
    image: 'gifs/embeddable_scripts.gif',
    scripting: true,
    body: (
      <>
        <p>
          Scala CLI can be set up in shebang lines, making your *.scala or *.sc
          (or even .java or .md!) files runnable.
        </p>
        <p>
          Scala CLI supports piping inputs and is designed to be embeddable in
          other scripts, turning Scala into proper scripting language.
        </p>
      </>
    ),
  },
  {
    id: 'self-contained-examples',
    navLabel: 'Self-contained',
    title: 'Self-contained examples',
    image: 'gifs/self-contained-examples.gif',
    prototyping: true,
    body: (
      <>
        <p>
          With Scala CLI, configuration can be included in source code so
          complex examples can be self-contained and shipped as e.g. gist.
          Moreover, Scala CLI can compile, run and test gists without any manual
          work!
        </p>
        <p>Scala CLI is the perfect tool to submit and reproduce bugs.</p>
      </>
    ),
  },
];

export function getFeatures(filterId?: UseCaseId): FeatureItem[] {
  if (!filterId) return featuresCatalog;
  return featuresCatalog.filter(f => Boolean(f[filterId]));
}

/** @deprecated prefer getFeatures + FeaturesSection */
export default function allFeatures(filterId?: UseCaseId) {
  return getFeatures(filterId).map(f => (
    <ImageBox
      key={f.id}
      id={f.id}
      image={f.image}
      title={f.title}
      education={f.education}
      scripting={f.scripting}
      prototyping={f.prototyping}
      projects={f.projects}
    >
      {f.body}
    </ImageBox>
  ));
}
