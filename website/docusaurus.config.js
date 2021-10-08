/** @type {import('@docusaurus/types').DocusaurusConfig} */

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

module.exports = {
  title: 'Scala CLI',
  tagline: 'More featureful Scala Command-Line',
  url: 'https://scala-cli.virtuslab.org/',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'Virtuslab',
  projectName: 'scala-cli',
  plugins: ['docusaurus-plugin-sass'],
  themeConfig: {
    prism: {
      theme: lightCodeTheme,
      darkTheme: darkCodeTheme,
      additionalLanguages: ['java', 'scala', 'bash'],
    },
    navbar: {
      title: 'Scala CLI',
      logo: {
        alt: 'Scala Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          to: 'install',
          label: 'Installation'
        },
        {
         // type: 'doc',
          label: "Use cases",
          to: '/#use_cases',
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
          to: '/docs/cookbooks/index',
          label: 'Cookbook'
        },
        {
          type: 'doc',
          docId: 'overview',
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
         // customCss: require.resolve('./src/css/custom.css'),
          customCss: [require.resolve('./src/scss/style.scss')],
        },
      },
    ],
  ]
};
