/** @type {import('@docusaurus/types').DocusaurusConfig} */

let isCI = process.env.CI || false;

module.exports = {
  title: 'Scala CLI',
  tagline: 'Your New Shiny Scala Command-Line',
  url: 'https://virtuslab.github.io',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'VirtusLab',
  projectName: 'scala-cli',
  themeConfig: {
    navbar: {
      title: 'Scala CLI',
      logo: {
        alt: 'Scala Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'doc',
          docId: 'installation',
          position: 'left',
          label: 'Docs',
        },
        {
          href: 'https://github.com/VirtusLab/scala-cli',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Docs',
              to: '/docs/installation',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Gitter',
              href: 'https://gitter.im/VirtusLab/scala-cli',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/VirtusLab/scala-cli',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Scala CLI contributors.`,
    },
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl:
            'https://github.com/VirtusLab/scala-cli/edit/master/website/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};

if (isCI) {
  module.exports.baseUrl = '/scala-cli/';
} else {
  module.exports.baseUrl = '/';
}
