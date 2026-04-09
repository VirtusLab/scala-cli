---
name: scala-cli-deprecating-features
description: Deprecate CLI options, option aliases, using directives, sub-commands, or config keys in Scala CLI. Use when marking a feature as deprecated with a warning.
---

# Deprecating features (Scala CLI)

All deprecation mechanisms emit aggregated warnings (single consolidated message) via `Logger.deprecationWarning` / `Logger.flushDeprecationWarnings`, respecting suppression via `--suppress-deprecated-warnings` or `config suppress-warning.deprecated-features true`.

## Warning format

The formatter always prefixes with the exact name used and the feature type:

- Single: `` [warn] `--foo` option is deprecated. Use --bar instead.\nDeprecated features may be removed in a future version. ``
- Multiple: consolidated bullet-point list with each entry prefixed by name and type

The `message`/`detail` passed by callers should NOT repeat the feature name — only provide the reason or migration hint. Pass `""` for no extra detail.

## 1. Deprecate a CLI option (entire option)

In the options case class, add `@Tag(tags.deprecatedOption("detail"))` or `@Tag(tags.deprecatedOption)` (no detail):

```scala
@Tag(tags.deprecatedOption("Use --bar instead."))
  foo: Option[Boolean] = None,
```

- Fires for **any** name/alias of the option — the exact alias used is shown in the warning
- Detected in `RestrictedCommandsParser` via `arg.isDeprecatedOption`

## 2. Deprecate a CLI option alias

Add `@Tag(tags.deprecated("aliasName"))` alongside the `@Name("aliasName")`:

```scala
@Name("oldAlias")
@Tag(tags.deprecated("oldAlias"))
  myOption: Option[Boolean] = None,
```

- Fires only when the specific alias is used

## 3. Deprecate a using directive (key or value)

Add an entry to `DeprecatedDirectives.deprecatedCombinationsAndReplacements` in `modules/build/.../preprocessing/DeprecatedDirectives.scala`:

```scala
// Key swap (e.g. lib → dep):
DirectiveTemplate(Seq("oldKey"), None) -> keyReplacement("newKey")(
  deprecatedWarning("oldKey", "newKey")
)

// Deprecated for removal (no replacement):
DirectiveTemplate(Seq("removedKey"), None) -> noReplacement(
  deprecatedWarningForRemoval("removedKey")
)
```

- `keyReplacement` / `valueReplacement` — swap to a new key or value, emits a `TextEdit` for IDE quick-fix
- `noReplacement` — deprecated for removal, no `TextEdit` offered
- Emitted as a positioned `Diagnostic` (supports BSP with source locations)
- Not aggregated (kept as individual diagnostics for IDE support)

## 4. Deprecate a sub-command

Override `deprecationMessage` in the command object (detail only, name is auto-prefixed):

```scala
object MyCommand extends ScalaCommand[MyOptions] {
  override def deprecationMessage: Option[String] =
    Some("Use other-command instead.")
}
```

For deprecating only a specific command alias, override `deprecatedNames`:

```scala
override def deprecatedNames: Set[List[String]] = Set(List("old-alias"))
```

## 5. Deprecate a config key

Pass `deprecationMessage` to the `Key` constructor (currently supported on `BooleanEntry`):

```scala
val myKey = new Key.BooleanEntry(
  prefix = Seq("my"),
  name = "key",
  specificationLevel = SpecificationLevel.IMPLEMENTATION,
  description = "...",
  deprecationMessage = Some("Use my.new-key instead.")
)
```

- Warning emitted in `Config.scala` when the key is accessed

## Post-deprecation checklist

1. Run `./mill -i __.compile`
2. Run relevant tests
3. Run `./mill -i 'generate-reference-doc[]'.run` (deprecated options/aliases are marked in reference docs)
4. Update user-facing documentation if needed
