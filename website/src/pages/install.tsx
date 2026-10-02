import Layout from '@theme/Layout';
import {MDXProvider} from '@mdx-js/react';
import MDXComponents from '@theme/MDXComponents';
import Section from '../components/Section';
import BigHeader from '../components/BigHeader';
import BasicInstall from '../components/BasicInstall';
import InstallStickyNav from '../components/InstallStickyNav';
import AdvancedInstallation from '../../docs/_advanced_install.mdx';

const INSTALL_TOC = [
  {id: 'quick-start', navLabel: 'Quick start'},
  {id: 'scala-3-installation', navLabel: 'Scala 3'},
  {id: 'advanced-installation', navLabel: 'Advanced'},
  {id: 'standalone-launcher', navLabel: 'Standalone'},
  {id: 'bootstrapped-standalone-fat-jar', navLabel: 'Fat JAR'},
  {id: 'shell-completions', navLabel: 'Completions'},
  {id: 'scala-js-scala-native', navLabel: 'JS / Native'},
  {id: 'uninstall-scala-cli', navLabel: 'Uninstall'},
];

export default function InstallPage() {
  return (
    <Layout title="Install Scala CLI" description="How to install Scala CLI">
      <div className="sc-page sc-install-page content">
        <InstallStickyNav items={INSTALL_TOC} />

        <Section
          id="quick-start"
          band
          className="sc-hero-glow relative overflow-hidden"
        >
          <div
            className="sc-dot-grid pointer-events-none absolute inset-0"
            aria-hidden
          />
          <div className="sc-section-y relative grid gap-5 md:grid-cols-3 md:items-start md:gap-6">
            <div>
              <BigHeader
                title="Quick start"
                promptsign
                eyebrow="Installation"
                light
                className="mb-6"
              />
            </div>
            <div className="md:col-span-2">
              <BasicInstall />
            </div>
          </div>
        </Section>

        <MDXProvider components={MDXComponents}>
          <AdvancedInstallation />
        </MDXProvider>
      </div>
    </Layout>
  );
}
