import Link from '@docusaurus/Link';
import {ExternalLink} from 'lucide-react';
import ScalaCliWordmark from '../../components/ScalaCliWordmark';

type FooterLink = {
  label: string;
  to?: string;
  href?: string;
};

type FooterGroup = {
  title: string;
  links: FooterLink[];
};

const linkGroups: FooterGroup[] = [
  {
    title: 'Product',
    links: [
      {label: 'Install', to: '/install'},
      {label: 'Use cases', to: '/education'},
      {label: 'Documentation', to: '/docs/overview'},
      {label: 'Commands', to: '/docs/commands/basics'},
      {label: 'Guides', to: '/docs/guides/intro'},
      {label: 'Cookbook', to: '/docs/cookbooks/intro'},
    ],
  },
  {
    title: 'Community',
    links: [
      {label: 'Discord', href: 'https://discord.gg/ScreHFr957'},
      {
        label: 'GitHub Discussions',
        href: 'https://github.com/VirtusLab/scala-cli/discussions',
      },
      {
        label: 'Issues',
        href: 'https://github.com/VirtusLab/scala-cli/issues',
      },
    ],
  },
  {
    title: 'More',
    links: [
      {label: 'GitHub', href: 'https://github.com/VirtusLab/scala-cli'},
      {label: 'VirtusLab', href: 'https://virtuslab.com/'},
      {
        label: 'Scala 3',
        href: 'https://www.scala-lang.org/',
      },
    ],
  },
];

function FooterAnchor({link}: {link: FooterLink}) {
  if (link.href) {
    return (
      <a
        href={link.href}
        target="_blank"
        rel="noopener noreferrer"
        className="sc-footer-link sc-footer-link--external"
      >
        {link.label}
        <ExternalLink
          className="sc-footer-link__external"
          size={12}
          strokeWidth={2.25}
          aria-hidden
        />
      </a>
    );
  }
  return (
    <Link to={link.to!} className="sc-footer-link">
      {link.label}
    </Link>
  );
}

export default function Footer() {
  return (
    <footer className="sc-footer" data-name="footer">
      <div className="sc-footer__frame">
        <div className="sc-footer__main">
          <div className="sc-footer__brand">
            <Link to="/" className="sc-footer__logo" aria-label="Scala CLI home">
              <ScalaCliWordmark className="sc-footer-wordmark" />
            </Link>
            <p className="sc-footer__tagline">
              Command-line tool to interact with the Scala language — compile,
              run, test, and package.
            </p>
            <a
              className="sc-hero-vl group inline-flex items-center gap-3 no-underline"
              href="https://virtuslab.com/"
              target="_blank"
              rel="noopener noreferrer"
            >
              <span className="font-mono text-[0.7rem] font-medium tracking-[0.1em] text-white/50 uppercase transition-colors group-hover:text-white/75">
                Developed &amp; Maintained by
              </span>
              <img
                className="sc-hero-vl-logo h-7 w-auto opacity-90 transition-opacity group-hover:opacity-100"
                src="/img/virtuslab-logo.svg"
                alt="VirtusLab"
                width={110}
                height={30}
              />
            </a>
          </div>

          <div className="sc-footer__columns">
            {linkGroups.map(group => (
              <div key={group.title} className="sc-footer__col">
                <p className="sc-footer__col-title">{group.title}</p>
                <ul className="sc-footer__list">
                  {group.links.map(link => (
                    <li key={link.label}>
                      <FooterAnchor link={link} />
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="sc-footer__bottom">
          <p className="sc-footer__copyright">
            Copyright © 2021–{new Date().getFullYear()} VirtusLab Sp. z o.o. All
            rights reserved.
          </p>
          <nav className="sc-footer__legal" aria-label="Footer legal">
            <FooterAnchor
              link={{
                label: 'License',
                href: 'https://github.com/VirtusLab/scala-cli/blob/main/LICENSE',
              }}
            />
            <FooterAnchor
              link={{
                label: 'About VirtusLab',
                href: 'https://virtuslab.com/about-us',
              }}
            />
          </nav>
        </div>
      </div>
    </footer>
  );
}
