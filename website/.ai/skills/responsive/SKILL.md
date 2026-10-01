---
name: responsive
description: >-
  Mobile/tablet responsive rules for Scala CLI website (996px Infima breakpoint,
  navbar collapse, overflow, sticky chrome). Use when editing mobile CSS or
  product/docs layout at small viewports.
---

# Responsive / mobile

Docusaurus / Infima mobile breakpoint: **`996px`**. Desktop (≥997px) is the polished baseline — **do not regress it**.

## Breakpoints

| Range | Intent |
|-------|--------|
| ≥997px | Full navbar links, desktop docs sidebar, Features underline tabs (≥1024 for jump tabs) |
| ≤996px | Hamburger + drawer; icon/compact chrome; docs sidebar in navbar drawer |
| ≤767px | Install `headerTabs` stack; softer section titles already via `min-width` scales |
| ≤576px | Tighter `--sc-page-gutter`; icon-only search; slightly tighter section padding |

Shared height for search / GitHub / theme switch: **`--sc-navbar-control-h: 2.25rem`** (all viewports). Theme switch width stays **`--sc-theme-switch-w: 3.75rem`** on mobile — **do not squeeze**.

Right cluster order (DOM + visual): **Search → GitHub → Theme switch**.

Mobile search (≤996):

- Dedicated **square Search button** (`.sc-navbar-search-btn`, Lucide `Search`) in flex flow before GitHub — not a disguised input.
- Click opens `.sc-navbar-search--open`: fixed top panel with the real SearchBar + close (X) + backdrop. Escape / backdrop closes.
- Override Docusaurus `.navbarSearchContainer { position: absolute }` so the closed field stays out of chrome (clipped) until open.
- Desktop: hide the square button; keep the Metronic text field.

## Navbar collapse (critical)

Product CSS sets `.navbar .navbar__item { display: inline-flex }`, which **beats** Infima’s mobile `display: none`. Always restore:

```css
@media (max-width: 996px) {
  .navbar .navbar__item { display: none !important; }
  .navbar .navbar__toggle { display: inline-flex !important; }
}
```

Do not hide `ThemeSwitch` on mobile via leftover Infima `.colorModeToggle { display: none }` in `Navbar/Content/styles.module.css`.

Mobile drawer: style `.navbar-sidebar` links like product chrome; docs tree uses `.navbar-sidebar .theme-doc-sidebar-menu` (same Metronic language as desktop sidebar). On docs pages the drawer opens on the **secondary** panel (docs tree + “← Back to main menu”); primary site links are one tap away. Drawer header: our `ThemeSwitch` + square `.sc-navbar-sidebar-close` (Lucide `X`) — swizzle `Navbar/MobileSidebar/Header`.

**Critical — do not put `backdrop-filter` / `filter` / `transform` on `.navbar` itself.** Those create a containing block for `position: fixed` children, so the drawer and search panel clip to the ~64px navbar height and appear “under” the page. Keep glass blur on `.navbar::before` instead. Drawer/backdrop also need explicit z-index above content (`--ifm-z-index-fixed` + 1/2).

Right cluster ≤996: hide GitHub label/external; dedicated square Search button before GitHub opens a fixed top search panel (`NavbarSearchControl`).

## Product layout

- Grids already collapse via Tailwind (`md:grid-cols-*`). Prefer that over custom mobile grids.
- Hero CTAs: full-width stack ≤996 when needed.
- Segmented OS/method tabs: `flex-wrap`, `max-width: 100%`.
- **Overflow:** CSS grid items default `min-width: auto` — long code in Install `headerTabs` (`display: contents`) blows the page. Set `min-width: 0; max-width: 100%` on panel wrappers and code containers.

## Sticky jump nav (Features + Install)

- `position: fixed; top: 0; padding-top: var(--ifm-navbar-height)` so background sits under the navbar.
- Translucent glass (`~0.72` alpha) + `backdrop-filter` blur so content shows through.
- `::before` extends upward to cover macOS rubber-band gaps.
- Shadow only under the tab strip (`::after`), not around the under-navbar pad.
- Desktop tabs from **1024px**; below that: mobile select + progress.

Do **not** go back to `top: var(--ifm-navbar-height)` alone — sticky header + fixed bar separate on overscroll.

## Docs mobile

- Desktop sidebar `display: none`; open via navbar hamburger → secondary panel + back control.
- Soft TOC collapsible; breadcrumbs may ellipsize long crumbs.
- Tables/code: horizontal scroll inside the component, not the page.

## Section padding on small screens

`sc-section-y` uses mobile-first padding then `min-width` bumps. Optional tighter overrides only inside `max-width: 996px` / `576px` — never change the desktop `min-width: 1024px` values when “fixing mobile.”
