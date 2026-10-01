---
name: website-tailwind-docusaurus
description: Tailwind v4 + Infima coexistence in Docusaurus. Use when styling, adding utilities, or debugging CSS conflicts.
---

# Tailwind + Docusaurus

## Setup

- Plugin: `src/plugins/tailwind-config.ts` → `configurePostCss` + `@tailwindcss/postcss`
- Entry: `src/css/tokens.css` imported via `customCss` in `docusaurus.config.ts`
- Dark: `@custom-variant dark (&:where([data-theme=dark], [data-theme=dark] *))`

## Do

- Prefer utilities + `cn()` from `src/lib/utils.ts`
- Use Infima grid (`row` / `col`) only where existing page structure still relies on it; new layouts prefer Tailwind grid/flex
- Scope product chrome with clear class prefixes (`sc-` optional) if Infima clashes

## Do not

- Use bare Tailwind class `container` (collides with Infima) — use `mx-auto w-full max-w-[1204px] px-4` (or similar)
- Enable aggressive Tailwind preflight that resets Infima docs styles — keep theme + utilities; verify docs after CSS changes
- Add `tailwind.config.js` for simple tokens — CSS `@theme` is source of truth
