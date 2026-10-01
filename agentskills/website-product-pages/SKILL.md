---
name: website-product-pages
description: Product marketing pages for Scala CLI website. Use when editing landing, install, or use-case pages.
---

# Product pages

## Routes

| Route | File | Notes |
|-------|------|-------|
| `/` | `src/pages/index.tsx` | Landing |
| `/install` | `src/pages/install.tsx` | Quick start + `_advanced_install.mdx` |
| `/education` | `src/pages/education.tsx` | UseCase shell |
| `/scripting` | `src/pages/scripting.tsx` | UseCase shell |
| `/prototyping` | `src/pages/prototyping.tsx` | UseCase shell |
| `/projects` | `src/pages/projects.tsx` | UseCase shell |

## Copy freeze

Do not edit visible strings, feature blurbs, or MDX docs prose. Redesign layout/classes/components only.

## Shared building blocks

`YellowBanner`, `Section`, `SectionAbout`, `IconBox`, `UseCaseTile`, `BigHeader`, `SmallHeader`, `BasicInstall`, `ImageBox`, `features`, `UseCase`.

## Layout rules (Home + bands)

**Source of truth:** [home-layout](../website-home-layout/SKILL.md) — sections/grid/separators, components, navbar/footer/jump navs.

When changing Home spacing, grids, or chrome, read that skill first and keep bands on `sc-section-y` + the shared 3-col grid.

Docs work: [docs-chrome](../website-docs-chrome/SKILL.md). Mobile: [responsive](../website-responsive/SKILL.md).