---
name: website-react-components
description: TSX component conventions for the Scala CLI Docusaurus website.
---

# React components (website)

## Conventions

- Files: PascalCase `.tsx` (helpers `.ts`)
- Typed props (`type` / `interface`); `ReactNode` for children
- Style with Tailwind + `cn()` from `@site/src/lib/utils` or relative `../lib/utils`
- Inline Tailwind in JSX — do not export static layout `*ClassName` constants
- Keep components small; extract helpers to `*.ts` when logic grows

## MDX-facing helpers

`MarkdownComponents.tsx` (`ChainedSnippets`, `GiflikeVideo`) and `DownloadButton.tsx` are imported from docs. Prefer extensionless imports from MDX. Keep `.runnable-command` styles in `tokens.css`.

## Theme / swizzles

`src/theme/Root.tsx` — GTM / document shell only unless swizzling more chrome.

Local / swizzled chrome (do not revert to stock Infima look):

| Path | Role |
|------|------|
| `Navbar/Content` | Brand, search, GitHub, theme switch, mobile toggle |
| `Navbar/Logo` | Wordmark brand link |
| `NavbarItem/DropdownNavbarItem/Desktop` | Custom chevron dropdown |
| `Footer` | Product footer + external icons on `href` |
| `DocBreadcrumbs` (+ `Items/Home`) | Soft chips, clickable parents, House icon |
| `PaginatorNavLink` | Lucide chevrons instead of `«`/`»` |

Also under `src/components/`: `ThemeSwitch`, `NavbarDevTools`, `ScalaCliWordmark`, `HomeNavbarEffect`, sticky navs.

Product layout: [home-layout](../website-home-layout/SKILL.md). Docs: [docs-chrome](../website-docs-chrome/SKILL.md). Mobile: [responsive](../website-responsive/SKILL.md).

New files under `src/theme/**` often need a **yarn start restart**.

## Icons

Prefer **lucide-react** for product UI (benefits, use cases, chrome). Pass `LucideIcon` into `IconBox`; map use-case slugs in `UseCaseTile`. Do not reintroduce PNG icons for those cards.