# Home — chrome (navbar, footer, features / install jump nav)

## Navbar layout

Right cluster order (LTR):

1. Search  
2. GitHub  
3. Separator (`.sc-navbar-sep`)  
4. Theme switch  

Logo–nav gap is tight (brand `margin-right` ~`0.25rem`).

Vertical alignment: navbar is flex-centered, `padding-top/bottom: 0`, height `--ifm-navbar-height` (`4rem`). Links/controls share one mid-line; avoid stacking `min-height: navbar` on inner flex children (fights Infima padding).

Shared control height: **`--sc-navbar-control-h: 2.25rem`** for search, GitHub, and theme switch (desktop + mobile).

Nav link vs dropdown: same visual level (padding/line-height). Dropdown hover: keep open across the gap with `::before`/`::after` bridges (no vanish when moving pointer into the menu).

### Search

Desktop: Metronic-like field — soft fill, height = `--sc-navbar-control-h`, search icon left, OS key hints right (style keys only — not the hint container as a key).

Mobile (≤996): square **Search** button (`.sc-navbar-search-btn`) before GitHub. Tap opens fixed top panel (`.sc-navbar-search--open`) with the real SearchBar, close (X), backdrop. Escape closes. Implemented by `NavbarSearchControl`.

### GitHub

Dark pill (inverts in dark theme); mark + label + Lucide `ExternalLink`. ≤996px: icon-only square (`width/height: var(--sc-navbar-control-h)`).

### Theme switch

Track with faint Sun/Moon; **thumb shows active icon** (Sun in light, Moon in dark). Soft-square thumb (`--sc-theme-switch-thumb`), not a pill. Width `--sc-theme-switch-w: 3.75rem` on **all** breakpoints — do not narrow on mobile. Travel = `width - thumb - 2*pad` via CSS vars.

### Home transparent header

- `data-sc-home` + not `data-sc-nav-solid`: glass-clear over hero, light text/wordmark.
- After scroll (`data-sc-nav-solid`): solid bar, ink wordmark (white in dark theme).
- Only **first** `.sc-page > section.sc-hero-glow:first-of-type` uses negative top margin under the navbar (prefer `:first-of-type` so a leading `<nav>` TOC does not break it). Later glow bands (Get Started) must not.

### Active nav item

Header **Docs** is the only docs-tree active state. Commands / Guides / Cookbook use `activeBaseRegex: '^$'` in `docusaurus.config.ts`.

### Dropdown

Menu sits slightly below the trigger; hover bridge so the menu does not close in the gap. Chevron optically nudged to text baseline. Card/link underline rules for use-case tiles do not apply to navbar links.

### Mobile (≤996px)

See [responsive](../website-responsive/SKILL.md). Must hide `.navbar__item` with `!important` (our `inline-flex` otherwise wins). Drawer: `.navbar-sidebar` styled like product menus; docs secondary panel uses Metronic sidebar language. Header uses our `ThemeSwitch` + square Lucide `X` close (`.sc-navbar-sidebar-close`), not Infima ColorModeToggle / IconClose.

Glass blur must live on `.navbar::before`, **not** on `.navbar` — `backdrop-filter` on the nav creates a containing block that clips the fixed mobile drawer to navbar height.

## Footer

- Dark band `#0b0f19`.
- Brand column wider than link columns (~`flex: 1.6`, max ~34rem); tagline can breathe.
- Wordmark text **white** (overrides navbar theme selectors).
- VL line under tagline; copyright + legal in bottom row.
- **Every `href` (external) link** gets Lucide `ExternalLink` (columns + legal). Internal `to` links: no icon.
- Legal: License → GitHub LICENSE; About VirtusLab → `https://virtuslab.com/about-us`.

## Jump nav (Features + Install)

Shared classes: `.sc-features-nav` / `.sc-jump-nav` (+ `--visible`).

| Concern | Rule |
|---------|------|
| Placement | `fixed; top: 0; padding-top: var(--ifm-navbar-height)` (under higher-z navbar) |
| Glass | ~`0.72` alpha + `backdrop-filter` blur — content must show through |
| Overscroll | `::before` fills upward; avoids macOS rubber-band gap vs sticky header |
| Shadow | Only under tab strip (`::after`), not full chrome including under-navbar pad |
| Show when | Header passed section title / hero; section still in view |
| Hide when | Above trigger or past section/page bottom |
| Active item | Scroll probe at nav bottom — last section top ≤ probe |
| Click | Lock active id ~900ms during smooth scroll |
| Desktop tabs | ≥1024px underline tabs; below: select + progress |

## Tokens touchpoints

Most chrome lives in `src/css/tokens.css`:

- `.sc-navbar-*`, `.sc-theme-switch*`, `--sc-navbar-control-h`, `--sc-theme-switch-*`
- `.sc-footer*`, `.sc-footer-link--external`
- `.sc-features-nav*`, `.sc-jump-nav*`
- `.sc-section-y`, `.sc-band-*`, `.sc-prompt`, `.sc-wordmark-text`
- `a.sc-use-case-tile*`
- mobile `@media (max-width: 996px)` / `576px` blocks at file end
