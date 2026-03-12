---
name: scala-cli-adding-directives
description: Add or change using directives in Scala CLI. Use when adding a new //> using directive, registering a directive handler, or editing directive preprocessing.
---

# Adding a new directive (Scala CLI)

1. **Create a case class** in `modules/directives/src/main/scala/scala/build/preprocessing/directives/` extending one of:
   - `HasBuildOptions` — produces `BuildOptions` directly
   - `HasBuildOptionsWithRequirements` — produces `BuildOptions` with scoped requirements (e.g. `test.dep`)
   - `HasBuildRequirements` — produces `BuildRequirements` (for `//> require`)

2. **Annotate**: `@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)`, `@DirectiveDescription("…")`, `@DirectiveUsage("…")`, `@DirectiveExamples("…")`, `@DirectiveName("key")` on fields.

3. **Companion**: `val handler: DirectiveHandler[YourDirective] = DirectiveHandler.derive`

4. **Register** in `modules/build/.../DirectivesPreprocessingUtils.scala` in the right list: `usingDirectiveHandlers`, `usingDirectiveWithReqsHandlers`, or `requireDirectiveHandlers`.

5. **Regenerate reference docs**: `./mill -i 'generate-reference-doc[]'.run`

CLI options always override directive values when both set the same thing.
