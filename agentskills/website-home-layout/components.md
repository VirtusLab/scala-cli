# Home — product components

## Building blocks

| Component | Role |
|-----------|------|
| `YellowBanner` | Hero: eyebrow, title, copy, optional CTAs/VL. With `image` → 2-col + console media; **without `image`** → text-only (Use Cases). Optional `eyebrow` (default `Scala CLI`). |
| `Section` | Full-bleed band + `sc-frame` |
| `SectionAbout` | Why / Install: eyebrow + heading + prose. Props: `layout` (`split` 1+2 default, `stack` title-top full-width), `headerTabs` (hoist first Tabs into header), `titles` / `titleJoin` (`slash`\|`amp`), `toc` / `navLabel`, `eyebrow`, `promptsign`. |
| `InstallStickyNav` | Install page jump TOC (same chrome as Features nav). |
| `SectionEyebrow` / `SectionHeading` | Mono eyebrow + Merriweather title |
| `BigHeader` / `SmallHeader` | Section intros (Use cases / Features / Get Started) |
| `IconBox` | Benefit card — Lucide icon, **not** a link (no hover chrome) |
| `UseCaseTile` | Use-case card — link when `slug` set; dashed/static when `slug={false}` |
| `InstallBand` | Full-bleed plain/soft wrapper for install `SectionAbout` blocks (`tone="plain" \| "soft"`). |
| `ImageBox` | Feature row: copy + console image (1+1) |
| `FeaturesSection` / `FeaturesStickyNav` | Features list + sticky jump nav |
| `ScalaCliWordmark` | Mark + live SVG text “Scala CLI” |
| `ThemeSwitch` | Navbar light/dark switch |
| `Cta` | Primary / secondary / yellow / ghost buttons |

## `>_` prompt (`.sc-prompt`)

- Pseudo-element `::_before { content: ">_" }` on titles with `promptsign`.
- Size ~`0.78em`, monospace, brand color, **subtle** (`opacity ~0.72`, weight 500).
- Bottom-aligned (`bottom: ~0.28em`), padding-left ~`0.92em` (close to text, not huge).
- Graphic accent — must not dominate the heading.

## Wordmark (`ScalaCliWordmark`)

- Mark from logo paths + Merriweather Variable text (`font-size` 21 in viewBox, gap ~11 from mark).
- Navbar height ~`1.5rem`; footer slightly larger.
- Text fill follows chrome state (navbar transparent/solid/dark; **footer always white** — beat home/nav overrides with higher specificity under `.sc-footer`).

## IconBox

- Prop: `icon?: LucideIcon` (e.g. `Hand`, `Rocket`, `Terminal`).
- Compact 40×40 soft tile: brand tint bg, inset ring, icon stroke ~1.75.
- **No hover effects** — cards are not interactive.

## UseCaseTile

- Icons by slug: `GraduationCap`, `FileCode2`, `FlaskConical`, `Package`; inactive → `MessageSquarePlus`.
- Linked tiles (`slug` string): wrap in `a.sc-use-case-tile`; hover lift/border/shadow OK.
- Inactive (`slug={false}`): dashed border, **no** hover.
- Title/description stay **normal ink colors** on the link (not Infima link blue/red); **no full-card underline** on hover — see `a.sc-use-case-tile` rules in `tokens.css`.
- “Read more” uses brand mono + Lucide `ArrowRight`.

## Features

- Sticky/fixed nav appears only after site header has scrolled over the Features **title** (`.sc-features-title`); hides when leaving the section.
- Active tab = last feature whose top has passed under the nav bottom (scroll probe), not jumpy IntersectionObserver multi-hit.
- Desktop (≥1024): underline tabs; mobile: compact select + progress.
- Placement / glass / overscroll: see [chrome.md](./chrome.md) + [responsive](../website-responsive/SKILL.md).
- Each `ImageBox`: `md:grid-cols-2 md:gap-6` (1+1), same gap as site grid.

## Install sticky TOC

Same chrome as Features (`InstallStickyNav` / `.sc-jump-nav`) — appears after Quick start hero, hides near page end.

## CTAs / VL

- Product CTAs: `.sc-cta*` so Infima link styles cannot wash out button text.
- **Motion** (in `tokens.css`): shared transitions on color / bg / border / shadow / transform; hover lift `-2px` + stronger shadow; active settles down; `focus-visible` yellow ring. Respect `prefers-reduced-motion` (no transform).
- Keep animations subtle — no bounce, no scale pop, no infinite loops.
- **Hero** (`YellowBanner` + `showCtas`): primary `>_ Install` + **secondary** `Documentation` (ghost/outline on dark glow — white text, yellow border wash).
- **Get Started** band (same `.sc-hero-glow`): the “Full installation guide” CTA must use **`variant="secondary"`** — same look as Hero Documentation. Do **not** use `yellow` there (yellow fill fights the dark band language).
- `yellow` variant is for light surfaces if needed; prefer secondary on dark hero bands.
- VirtusLab attribution under hero CTAs **and** in footer brand column (shared visual language).
