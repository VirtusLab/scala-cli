# AI agent context — website

Stack: **Docusaurus 3**, React **TSX**, Tailwind CSS **v4**, Infima (docs chrome).

Task-specific skills live under repo-root [`agentskills/`](../agentskills/) (prefix `website-`), same convention as the rest of Scala CLI.

## Non-negotiables

1. **Stay inside `website/`** for website work unless a human explicitly expands scope.
2. **Copy freeze** — do not change user-facing strings on product pages or markdown/MDX docs content. Visual/layout/markup structure only (except mechanical import-path fixes for TS).
3. **Brand** — keep Scala CLI logo (`static/img/logo.svg`), brand red `#F23135`, accent yellow `#FFD583`, and the `>_` CLI motif.
4. **Tokens** live in [`src/css/tokens.css`](src/css/tokens.css) (`@theme`, `:root`, `[data-theme='dark']`) and map onto Infima `--ifm-*`.
5. **Product UI is TypeScript** (`.tsx` / `.ts`). Prefer Tailwind utilities + `cn()` over new SCSS.
6. **Desktop first** — ≥997px is polished. Mobile work must be scoped to `max-width` queries.
7. **Soft radii everywhere** — `0.5rem` / `--ifm-button-border-radius`. No pills.

## Skills

| Skill | When |
|-------|------|
| [website-design-tokens](../agentskills/website-design-tokens/SKILL.md) | Colors, type, gutters, motion, Infima mapping |
| [website-tailwind-docusaurus](../agentskills/website-tailwind-docusaurus/SKILL.md) | Styling, PostCSS plugin, dark theme |
| [website-product-pages](../agentskills/website-product-pages/SKILL.md) | Landing / install / use-case pages |
| [website-home-layout](../agentskills/website-home-layout/SKILL.md) | Home bands, grids, separators, chrome, Features nav |
| [website-docs-chrome](../agentskills/website-docs-chrome/SKILL.md) | Docs sidebar, breadcrumbs, admonitions, code, paginator |
| [website-responsive](../agentskills/website-responsive/SKILL.md) | ≤996px navbar, overflow, sticky glass, docs drawer |
| [website-react-components](../agentskills/website-react-components/SKILL.md) | TSX components under `src/` |

## Commands

```bash
yarn install
yarn start
yarn build
yarn typecheck
```

Prefer `yarn build:serve` when verifying local search / production CSS.
