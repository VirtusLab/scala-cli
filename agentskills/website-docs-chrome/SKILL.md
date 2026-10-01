---
name: website-docs-chrome
description: >-
  Docs chrome styling for Scala CLI website (sidebar, breadcrumbs, admonitions,
  code, TOC, paginator). Use when editing docs layout, tokens.css docs rules,
  or swizzled Doc*/Paginator theme files.
---

# Docs chrome

Product gutters and soft radii apply to docs. Source: `src/css/tokens.css`. Content under `docs/` is **copy-frozen**.

## Shared with product

| Token | Role |
|-------|------|
| `--sc-page-gutter` | Horizontal padding (docs container = product `.sc-frame`) |
| `--sc-docs-pad-y` | Docs main / sidebar top padding |
| `--sc-docs-sidebar-pad-x` | Sidebar horizontal pad |
| `--ifm-button-border-radius` / `0.5rem` | Buttons, breadcrumbs, alerts, pagination, code |

Docs main:

```css
main[class*='docMainContainer'] > .container {
  --ifm-spacing-horizontal: var(--sc-page-gutter);
  padding-inline: var(--sc-page-gutter);
  padding-block: var(--sc-docs-pad-y);
}
```

## Sidebar (Metronic-like)

Scope: `.theme-doc-sidebar-container` (desktop) and `.navbar-sidebar .theme-doc-sidebar-menu` (mobile drawer).

- Soft hover wash; active leaf: light brand tint + brand text (not Infima underline).
- Nested lists: quieter, slightly indented; active nested keeps brand text color.
- Sublist carets: quiet slate; no heavy Infima chevron chrome.
- Dark: ink surface, yellow active accents.

Do not restyle with Infima primary blue or thick active bars.

## Breadcrumbs

Swizzled: `src/theme/DocBreadcrumbs/` (+ `Items/Home`).

- Soft chip links (`border-radius: 0.5rem`), not pills.
- Category parents are **links** to the first sidebar leaf (`findFirstSidebarItemLink`) — not dead text.
- Home: Lucide `House` icon.
- Separator: Lucide `ChevronRight` (or styled `/`), muted.

## Admonitions (note / tip / info / warning / danger)

- `--ifm-alert-shadow: none` — **no card shadows**.
- Soft fill + 1px border + **3px left accent**; radius `0.5rem`.
- Heading: Geist sans, small uppercase, weight ~600 — not Infima heavy.
- Colors: slate note, sky info, emerald tip, brand-yellow warning/caution, brand-red danger (+ dark variants in `tokens.css`).

## Code blocks

- Unified chrome: soft border, radius, **no shadow** (docs + `.runnable-command`).
- Dark docs: quiet wash (`rgba(255,255,255,0.04)`), soft slate border — not bright cards.
- Overview `BasicInstall` (`.install-tabs` outside hero): dark theme uses **yellow** active OS switch (same language as hero glow). Install code chip is a quiet dark surface (not white). Hero `.sc-hero-glow` chips stay yellow-on-ink.
- Inside ChainedSnippets: outer `.runnable-command` is transparent — only the inner code block has chrome (no double box).
- Long lines: `max-width: 100%` + `overflow-x: auto` on content; grid parents need `min-width: 0` (Install `headerTabs`).

## TOC

- Desktop: `.theme-doc-toc-desktop` top aligned with `--sc-docs-pad-y`.
- Mobile: `.theme-doc-toc-mobile` Metronic disclosure — soft `#f9fafb` fill + border, Lucide `List` + label + `ChevronDown` (swizzle `TOCCollapsible/CollapseButton`), expanded links with soft hover / brand active (same language as docs sidebar).

## Paginator (Previous / Next)

Swizzled: `src/theme/PaginatorNavLink/index.tsx`.

- Lucide `ChevronLeft` / `ChevronRight` — hide Infima `«` / `»` (`::before`/`::after { content: none }`).
- Soft bordered cards, no shadow; hover brand border wash.
- Label row: `inline-flex` + chevron + title.

## Navbar on docs

Only **Docs** is active for `/docs/**` — Commands / Guides / Cookbook use `activeBaseRegex: '^$'` in `docusaurus.config.ts` so they are not “current” when browsing docs trees.
