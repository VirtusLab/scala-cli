/** @type {import('@docusaurus/types').DocusaurusConfig} */

let isCI = process.env.CI || false;

module.exports = {
  title: 'Scala CLI',
  tagline: 'More featureful Scala Command-Line',
  url: 'https://virtuslab.github.io',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'Virtuslab',
  projectName: 'scala-cli',
  themeConfig: {
    navbar: {
      title: 'Scala CLI',
      logo: {
        alt: 'Scala Logo',
        src: 'img/logo.png',
      },
      items: [
        {
         // type: 'doc',
          label: "Use cases",
          items: [
            {
              to: '/education',
              label: 'Education',
            },
            {
              to: '/scripting',
              label: 'Scripting',
            },
            {
              to: '/prototyping',
              label: 'prototyping, experimenting, reproducing',
            },
            {
              to: '/projects',
              label: 'Single-module projects',
            }
          ]
        },
        {
          type: 'doc',
          docId: 'installation',
          position: 'left',
          label: 'Documentation',
        },
        {
          href: 'https://github.com/Virtuslab/scala-cli',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Documentation',
              to: '/docs/overview',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Gitter',
              href: 'https://gitter.im/Virtuslab/scala-cli',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/Virtuslab/scala-cli',
            },
          ],
        },
      ],
      copyright: `Copyright © 2021 VirtusLab Sp. z. o. o.`,
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
            'https://github.com/Virtuslab/scala-cli/edit/master/website/',
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
