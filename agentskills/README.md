# Agent skills (Scala CLI)

This directory holds **agent skills** — task-specific guidance loaded on demand by AI coding agents. The layout is tool-agnostic; Cursor, Claude Code, Codex, and other tools that support a standard skill directory can use this (e.g. by configuring or symlinking to `.agents/skills/` if required).

Each subdirectory contains a `SKILL.md` with frontmatter and instructions. See [agentskills/agentskills](https://github.com/agentskills/agentskills) for the open standard.

## Skills

| Skill | Scope |
|-------|--------|
| [adding-directives](./adding-directives/SKILL.md) | New `//> using` directives |
| [deprecating-features](./deprecating-features/SKILL.md) | Deprecations / removals |
| [integration-tests](./integration-tests/SKILL.md) | CLI integration tests |
| [website-design-tokens](./website-design-tokens/SKILL.md) | Website colors, type, Infima tokens |
| [website-docs-chrome](./website-docs-chrome/SKILL.md) | Docs sidebar, breadcrumbs, admonitions |
| [website-home-layout](./website-home-layout/SKILL.md) | Home/install bands, grids, chrome |
| [website-product-pages](./website-product-pages/SKILL.md) | Landing / use-case pages |
| [website-react-components](./website-react-components/SKILL.md) | Website TSX conventions |
| [website-responsive](./website-responsive/SKILL.md) | ≤996px navbar / overflow |
| [website-tailwind-docusaurus](./website-tailwind-docusaurus/SKILL.md) | Tailwind + Docusaurus styling |
