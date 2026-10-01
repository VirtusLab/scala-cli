import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import {themes as prismThemes} from 'prism-react-renderer';
import path from 'path';

const config: Config = {
  title: 'Scala CLI',
  tagline:
    'Command-line tool to compile, run, test, and package Scala code — the default Scala runner.',
  url: 'https://scala-cli.virtuslab.org/',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },
  favicon: 'img/favicon.ico',
  organizationName: 'Virtuslab',
  projectName: 'scala-cli',
  plugins: [
    path.resolve(__dirname, 'src/plugins/tailwind-config.ts'),
    '@easyops-cn/docusaurus-search-local',
  ],
  themeConfig: {
    image: 'img/logo.png',
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'scala', 'bash'],
    },
    navbar: {
      // Wordmark SVG (mark + Merriweather) is rendered by theme/Navbar/Logo.
      title: '',
      logo: {
        alt: 'Scala CLI',
        src: 'img/scala-cli-wordmark.svg',
      },
      items: [
        {
          to: 'install',
          label: 'Install',
        },
        {
          type: 'doc',
          docId: 'overview',
          position: 'left',
          label: 'Docs',
        },
        {
          to: '/docs/commands/basics',
          label: 'Commands',
          // Docs section owns the active state — don't double-highlight
          activeBaseRegex: '^$',
        },
        {
          to: '/docs/guides/intro',
          label: 'Guides',
          activeBaseRegex: '^$',
        },
        {
          to: '/docs/cookbooks/intro',
          label: 'Cookbook',
          activeBaseRegex: '^$',
        },
        {
          label: 'Use cases',
          position: 'left',
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
              label: 'Prototyping & reproducing',
            },
            {
              to: '/projects',
              label: 'Single-module projects',
            },
          ],
        },
      ],
    },
    footer: {
      style: 'dark',
      // Custom Footer theme component owns layout; keep copyright for SEO/tools.
      copyright: `Copyright © 2021–${new Date().getFullYear()} VirtusLab Sp. z o.o.`,
      links: [],
    },
  } satisfies Preset.ThemeConfig,
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/Virtuslab/scala-cli/edit/main/website/',
        },
        theme: {
          customCss: ['./src/css/tokens.css'],
        },
      } satisfies Preset.Options,
    ],
  ],
};

export default config;
