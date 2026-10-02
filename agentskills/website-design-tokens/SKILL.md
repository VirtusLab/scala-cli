---
name: website-design-tokens
description: Scala CLI website design tokens (brand colors, typography, gutters, motion, Infima mapping). Use when styling or editing tokens.css.
---

# Design tokens

Source of truth: [`src/css/tokens.css`](../../website/src/css/tokens.css).

## Brand (do not change)

| Token | Value | Use |
|-------|-------|-----|
| `--color-brand` | `#F23135` | Primary accent, `>_` prompt, emphasis |
| `--color-brand-deep` | `#c41e22` | Prose / docs links (light) |
| `--color-brand-yellow` | `#FFD583` | Hero / dark accents / warning wash |
| Logo | `static/img/logo.svg` | Navbar / OG |

## Typography

- **Geist Variable** (`font-sans`) — UI, body, docs chrome
- **Merriweather Variable** (`font-heading` / `.sc-display`) — section titles + wordmark text
- **Geist Mono** — eyebrows, `>_`, code, footer labels
- Soft radii: **`0.5rem`** / `--ifm-button-border-radius` / `--ifm-global-radius` (not pills)
- Product CTAs: `.sc-cta*` so Infima cannot wash button text

## Surfaces

| Token / class | Role |
|---------------|------|
| `--color-surface` / `bg-surface` | Soft gray bands (`sc-band-soft`) |
| `bg-white` / `dark:bg-ink` | Plain bands (`sc-band-plain`) |
| `.sc-hero-glow` | Dark hero / Get Started |
| `--color-ink` `#0b0f19` | Dark surfaces / footer |

## Shared gutters & chrome sizes

| Token | Role |
|-------|------|
| `--sc-page-gutter` | Product `.sc-frame` + docs container (+ sticky nav inners) |
| `--sc-docs-pad-y` | Docs vertical pad / TOC offset |
| `--sc-docs-sidebar-pad-x` | Docs sidebar pad |
| `--sc-navbar-control-h` | `2.25rem` — search / GitHub / theme switch height |
| `--sc-theme-switch-w` / `--pad` / `--thumb` / `--travel` | Theme switch geometry (square thumb) |

Scale gutters with `min-width: 768px` / `1280px`. Do not invent parallel padding tokens.

## Infima overrides (high level)

- `--ifm-navbar-shadow: none`; glass navbar backgrounds
- `--ifm-alert-shadow: none`; alert radius/padding aligned to buttons
- Link colors → brand / yellow by theme
- Pagination: hide `«`/`»`; styled in docs-chrome + swizzled component

## Motion

- `--duration-interactive`: `300ms`
- `--ease-interactive`: `cubic-bezier(0.33, 1, 0.68, 1)`
- Respect `prefers-reduced-motion`

Layout rhythm: [home-layout](../website-home-layout/SKILL.md). Docs chrome: [docs-chrome](../website-docs-chrome/SKILL.md). Mobile: [responsive](../website-responsive/SKILL.md).
