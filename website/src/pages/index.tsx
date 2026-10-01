import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import {Hand, Rocket, Terminal} from 'lucide-react';
import Section from '../components/Section';
import IconBox from '../components/IconBox';
import SectionAbout from '../components/SectionAbout';
import UseCaseTile from '../components/UseCaseTile';
import BigHeader from '../components/BigHeader';
import BasicInstall from '../components/BasicInstall';
import YellowBanner from '../components/YellowBanner';
import FeaturesSection from '../components/FeaturesSection';
import {Cta} from '../components/Cta';

export default function Index() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout title={siteConfig.title} description={siteConfig.tagline}>
      <div className="sc-page content">
        <YellowBanner
          title="Scala CLI is a command-line tool to interact with the Scala language."
          image="gifs/demo.gif"
          showCtas
        >
          <p>
            It lets you compile, run, test, and package your Scala code (and
            more!)
          </p>
        </YellowBanner>

        <Section band className="sc-band-plain bg-white dark:bg-ink">
          <div className="sc-section-y">
            <SectionAbout title="Why Scala CLI?">
              <p>
                Scala CLI combines <em>all</em> of the features you need to learn
                and use Scala in your scripts, playgrounds and (single-module)
                projects.
              </p>
              <p>
                To get started you can read{' '}
                <a href="/docs/overview">the documentation</a>, or just{' '}
                <a href="/install">install</a> and enjoy <code>scala-cli</code>.
              </p>
              <p>
                Scala CLI is the default Scala runner and is being shipped as{' '}
                <code>scala</code> along with <code>scalac</code> as part of the
                official language installation since{' '}
                <a href="https://github.com/scala/scala3/releases/tag/3.5.0">
                  Scala 3.5.0
                </a>
                . (<a href="/docs/reference/scala-command/">read more</a>).
              </p>
            </SectionAbout>
          </div>
        </Section>

        <Section band className="sc-band-soft bg-surface dark:bg-ink-soft">
          <div className="sc-section-y grid gap-5 md:grid-cols-3 md:gap-6">
            <IconBox title="Intuitive, simple" icon={Hand}>
              <strong>
                No complicated mechanisms, tasks, plugins or extensions:
              </strong>{' '}
              just a single-module. All our commands have multiple aliases and
              follow well-known conventions.
            </IconBox>

            <IconBox title="Fast" icon={Rocket}>
              <strong>Scala CLI is optimized to be as fast as possible.</strong>{' '}
              CLI is compiled to native code and compilations are{' '}
              <a href="/docs/reference/bloop">offloaded to bloop</a>.
            </IconBox>

            <IconBox title="Command-line first" icon={Terminal}>
              <strong>
                Scala CLI does not require a configuration file, and all in-file
                configurations can be overridden by command-line.
              </strong>{' '}
              No additional installation or setup of an environment (such as a
              specific working directory) are required.
            </IconBox>
          </div>
        </Section>

        <Section id="use_cases" band className="sc-band-plain bg-white dark:bg-ink">
          <div className="sc-section-y">
            <BigHeader
              title="Who is Scala CLI designed for?"
              promptsign
              eyebrow="Use cases"
              className="mb-12"
            />

            <div className="grid gap-5 sm:grid-cols-2 md:grid-cols-3 md:gap-6">
              <UseCaseTile
                title="Education"
                slug="education"
                description="Scala CLI is a help — not a distraction — while learning Scala, a library or programming in general."
              />

              <UseCaseTile
                title="Scripting"
                slug="scripting"
                description="Scala CLI has all the tools to create (or be integrated into) scripts with the whole power of the Scala ecosystem."
              />

              <UseCaseTile
                title="Prototyping, Experimenting, Reproducing"
                slug="prototyping"
                description="With Scala CLI, experimenting with different libraries, Scala or JVM versions, or compiler options is easy and fun."
              />

              <UseCaseTile
                title="Single-module projects"
                slug="projects"
                description="Scala CLI provides all the tools you need to manage single-module projects like CLI or basic web applications, or server-less lambdas."
              />

              <UseCaseTile
                title="Your use case"
                slug={false}
                description={
                  <span>
                    If you see other use cases for Scala CLI, let us know using{' '}
                    <a href="https://github.com/VirtusLab/scala-cli/discussions/categories/ideas">
                      GitHub Discussions!
                    </a>
                  </span>
                }
              />
            </div>
          </div>
        </Section>

        <Section band className="sc-hero-glow relative overflow-hidden">
          <div className="sc-dot-grid pointer-events-none absolute inset-0" aria-hidden />
          <div className="sc-section-y relative grid gap-5 md:grid-cols-3 md:items-start md:gap-6">
            <div>
              <BigHeader
                title="Install Scala CLI"
                promptsign
                eyebrow="Get started"
                light
                className="mb-6"
              />
              <Cta href="/install" variant="secondary">
                Full installation guide
              </Cta>
            </div>
            <div className="md:col-span-2">
              <BasicInstall />
            </div>
          </div>
        </Section>

        <Section band className="sc-band-plain bg-white dark:bg-ink">
          <div className="sc-section-y">
            <FeaturesSection />
          </div>
        </Section>
      </div>
    </Layout>
  );
}
