# Agent instructions — Scala CLI website

Thin root [`AGENTS.md`](../AGENTS.md) → this file. Stack: **Docusaurus 3**, React **TSX**, Tailwind CSS **v4**, Infima (docs chrome).

## Non-negotiables

1. **Stay inside `website/`** for website work unless a human explicitly expands scope.
2. **Copy freeze** — do not change user-facing strings on product pages or markdown/MDX docs content. Visual/layout/markup structure only (except mechanical import-path fixes for TS).
3. **Brand** — keep Scala CLI logo (`static/img/logo.svg`), brand red `#F23135`, accent yellow `#FFD583`, and the `>_` CLI motif. Do **not** adopt Visdom emerald / Visdom wordmark.
4. **Tokens** live in [`src/css/tokens.css`](../src/css/tokens.css) (`@theme`, `:root`, `[data-theme='dark']`) and map onto Infima `--ifm-*`. Do not hardcode one-off hex in components when a token exists.
5. **Product UI is TypeScript** (`.tsx` / `.ts`). Prefer Tailwind utilities + `cn()` over new SCSS. Infima remains the docs stylesheet layer.
6. **No full shadcn Luma port** — light primitives (`cn`, optional CVA Button) only. Avoid Infima class collisions (never bare Tailwind `container`; use `max-w-* mx-auto`).
7. **Desktop first** — ≥997px is polished. Mobile work must be scoped to `max-width` queries; verify desktop after changes.
8. **Soft radii everywhere** — `0.5rem` / `--ifm-button-border-radius` for buttons, chips, alerts, pagination, code, search, theme thumb. No pills.

## Skills

| Skill | When |
|-------|------|
| [design-tokens](./skills/design-tokens/SKILL.md) | Colors, type, gutters, motion, Infima mapping |
| [tailwind-docusaurus](./skills/tailwind-docusaurus/SKILL.md) | Styling, PostCSS plugin, dark theme |
| [product-pages](./skills/product-pages/SKILL.md) | Landing / install / use-case pages |
| [home-layout](./skills/home-layout/SKILL.md) | Home bands, grids, separators, chrome, Features nav |
| [docs-chrome](./skills/docs-chrome/SKILL.md) | Docs sidebar, breadcrumbs, admonitions, code, paginator |
| [responsive](./skills/responsive/SKILL.md) | ≤996px navbar, overflow, sticky glass, docs drawer |
| [react-components](./skills/react-components/SKILL.md) | TSX components under `src/` |

Cursor also loads [`.cursor/rules/website.mdc`](../../.cursor/rules/website.mdc) when editing `website/**`.

## Layout

| Path | Role |
|------|------|
| `src/pages/` | Product routes (TSX) |
| `src/components/` | Product + MDX helpers |
| `src/css/tokens.css` | Design tokens + Infima + chrome + docs + mobile |
| `src/theme/` | Swizzled Docusaurus chrome |
| `src/lib/utils.ts` | `cn()` |
| `src/plugins/` | Docusaurus plugins (Tailwind PostCSS) |
| `docs/` | Documentation markdown (content freeze) |
| `static/img/` | Logo, GIFs, screenshots |

## Commands

```bash
yarn install
yarn start
yarn build
yarn typecheck
```

New theme swizzles often need a **dev-server restart** (hot reload may not pick up new `src/theme/**` files).
