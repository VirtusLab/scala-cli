---
name: website-home-layout
description: >-
  Homepage and product-page layout rules for Scala CLI website (sections,
  grids, chrome, features nav, icons). Use when editing Home, install,
  use-case pages, navbar, footer, or related product components.
---

# Home / product layout

 Canonical rules for `/` and shared product chrome. **Copy freeze** still applies — layout and styling only.

## Load these when relevant

| File | Contents |
|------|----------|
| [sections.md](./sections.md) | Band rhythm, 3-col grid, 1+2 / 1+1 splits, separators, padding |
| [components.md](./components.md) | Section*, IconBox, UseCaseTile, Features, wordmark, `>_` |
| [chrome.md](./chrome.md) | Navbar, theme switch, search/GitHub, footer, jump navs |

Also: [docs-chrome](../website-docs-chrome/SKILL.md), [responsive](../website-responsive/SKILL.md), [design-tokens](../website-design-tokens/SKILL.md).

## Hard constraints

1. Brand red `#F23135`, yellow `#FFD583`, logo mark + Merriweather wordmark, `>_` motif.
2. Soft radii (`rounded-lg` / `0.5rem`) — no pills.
3. Tokens in `src/css/tokens.css`; Lucide for UI icons (not PNG benefit/use-case icons).
4. Do not change user-facing copy.
5. Only the **first** `.sc-hero-glow` on a hero page pulls under the navbar (`:first-of-type`). Mid-page dark bands (Get Started) must not.
6. Desktop ≥997px stays intact; mobile rules are additive `max-width` overrides.
7. On `.sc-hero-glow`, outline CTAs use `variant="secondary"` (Hero Documentation style). Get Started “Full installation guide” must match — never solid `yellow` on that band.
8. Ambient glow: **first** `.sc-hero-glow` keeps inset red/yellow washes. Later `.sc-hero-glow` bands use edge-origin glows (top/bottom + sides) via `.sc-page > section.sc-hero-glow:not(:first-of-type)`. In **dark theme**, glow bands lift above page ink and get hairline seams vs plain/soft (see [sections.md](./sections.md)).

## Homepage section order

1. Hero (`YellowBanner` / `sc-hero-glow`)
2. Why (`SectionAbout`) — plain band
3. Benefits (`IconBox` ×3) — soft/gray band
4. Use cases (`UseCaseTile` grid) — plain band
5. Get Started (`BasicInstall`) — `sc-hero-glow`
6. Features (`FeaturesSection`) — plain band
7. Footer (site chrome)

## Install page (`/install`)

Same chrome/grid rules. Structure:

1. **Quick start** — `sc-hero-glow` + `sc-section-y` + 3-col **1+2** (`BigHeader` + `BasicInstall`).
2. **Advanced topics** — each topic (or related pair) in `InstallBand` alternating `tone="plain"` / `tone="soft"` (Home separator rules apply). Scala.js + Scala Native share one soft band.

See [sections.md](./sections.md) install checklist.
## Use-case pages (`/education`, `/scripting`, …)

1. **Hero** — `YellowBanner` **without** `image` (text-only, `eyebrow="Use case"`).
2. **Features** — plain band + `sc-section-y` + filtered `FeaturesSection`.

These routes are also in `HomeNavbarEffect` hero paths for the transparent navbar.
