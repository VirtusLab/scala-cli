# Home — sections, grid, separation, padding

## Band wrappers

Use `Section` with `band` so content sits in `.sc-bleed` → `.sc-frame` (max-width 1280px, shared horizontal padding).

### Tone classes

| Class | Use |
|-------|-----|
| `sc-band-plain` | White / `bg-white dark:bg-ink` |
| `sc-band-soft` | Gray surface / `bg-surface dark:bg-ink-soft` |
| `sc-hero-glow` | Dark glow band (Hero, Get Started) |

### Vertical rhythm

All major homepage bands wrap content in **`sc-section-y`**:

| Breakpoint | `padding-block` |
|------------|-----------------|
| default | `4.25rem` |
| `md` | `6rem` |
| `lg` | `7rem` |

Do **not** add ad-hoc `py-16` / `py-20` / `py-28` on band inners — use `sc-section-y`.

Trailing Infima margins must not steal the rhythm:

```css
.sc-section-y > :last-child,
.sc-section-y .sc-prose > :last-child { margin-bottom: 0; }
.sc-prose > :last-child { margin-bottom: 0; }
```

## Separators

- **White → soft** (`.sc-band-plain + .sc-band-soft`): visible seam (`border-top` + light inset/edge shadow).
- **Soft → white** (`.sc-band-soft + .sc-band-plain`): matching hairline `border-top`.
- **Dark → white / white → dark** (light theme): no extra separator — contrast is enough.
- **Glow ↔ plain/soft** (dark theme only): quiet top seam when entering glow; stronger hairline + falloff when leaving (hero bottom must cut through yellow ambient). Glow base also lifts (`#121826` → `#151b2b`).
- Do **not** put generic borders on every `.sc-bleed + .sc-bleed`.

## Shared content grid (3 columns)

At `md+`, product content aligns to **one 3-column grid**:

```
md:grid-cols-3 md:gap-6
```

(mobile: `gap-5`; often single column or `sm:grid-cols-2` before `md`.)

### Splits on that grid

| Pattern | Implementation | Used by |
|---------|----------------|---------|
| **1+2** | col 1 + `md:col-span-2` | Why (`SectionAbout`), Get Started |
| **1+1** | `md:grid-cols-2 md:gap-6` | Each Features row (`ImageBox`) |
| **1+1+1** | three equal cols | Benefits IconBoxes, Use case tiles |

Column edges of 1+2 must line up with the three IconBox / UseCase columns (same `gap-6`).

Do **not** use freestyle `1fr_2fr` unless it is explicitly the same as `grid-cols-3` + `col-span-2`.

## Homepage band checklist

| Section | Tone | Inner | Grid |
|---------|------|-------|------|
| Why | plain | `sc-section-y` → `SectionAbout` | 3-col, title / `col-span-2` prose |
| Benefits | soft | `sc-section-y grid … md:grid-cols-3 md:gap-6` | 3 IconBoxes |
| Use cases | plain | `sc-section-y` → BigHeader + tile grid | `md:grid-cols-3 md:gap-6` |
| Get Started | `sc-hero-glow` | `sc-section-y grid … md:grid-cols-3` | title col + `col-span-2` install |
| Features | plain | `sc-section-y` → `FeaturesSection` | Feature rows 1+1 |

### Install page checklist

| Section | Tone | Inner | Grid |
|---------|------|-------|------|
| Quick start | `sc-hero-glow` | `sc-section-y` | 3-col 1+2 (header + `BasicInstall`) |
| Advanced bands | plain/soft alt | `InstallBand` + `SectionAbout layout="stack"` | title top, full-width body; `headerTabs` when OS/shell Tabs |
| Dual titles | soft | `titles="A,B" titleJoin="amp"` | one section, `&` join |
| Standalone launcher | plain | `InstallBand` | 1+2 |
| Bootstrapped JAR | soft | `InstallBand` | 1+2 |
| Shell completions | plain | `InstallBand` | 1+2 |
| Scala.js + Native | soft | one `InstallBand`, two about blocks | 1+2 each |
| Uninstall | plain | `InstallBand` | 1+2 |

Alternate **plain / soft** via `InstallBand tone`. Do not dump all advanced blocks into a single plain band.

### Install `headerTabs` + code overflow

`SectionAbout` with `headerTabs` uses `display: contents` so the OS switch sits in the title row. Grid children default to `min-width: auto` — long fenced code **will expand the page**. Always keep:

- `.sc-about--header-tabs … .margin-top--md { min-width: 0; max-width: 100%; }`
- code containers `max-width: 100%; overflow-x: auto`

≤767px: tabs drop under the title (`grid-template-columns: 1fr`).

Use-case detail pages (`UseCase.tsx`): same `sc-section-y` + plain band under the hero.

## Features list padding

Outer band uses `sc-section-y`. Feature rows (`ImageBox`) keep internal vertical padding **between** items, but:

- first row: `padding-top: 0`
- last row: `padding-bottom: 0`

(so section padding alone owns the band edges — see `.sc-features-list > .sc-feature-block:first/last-child` in `tokens.css`).
