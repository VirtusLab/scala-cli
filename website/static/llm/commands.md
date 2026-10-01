# Scala CLI commands

Scala CLI runs a sub-command on inputs, using configuration from CLI flags and `//> using` directives. CLI options override directives.

Human docs: https://scala-cli.virtuslab.org/docs/commands/basics

## Important sub-commands

| Command | Purpose |
|---------|---------|
| `compile` | Compile sources (excluding tests) |
| `run` | Run code (also the default when inputs are passed) |
| `test` | Compile and run tests |
| `package` | Package as JAR or other formats |
| `repl` / `console` | Interactive Scala shell |
| `fmt` | Format code |
| `version` | Print version information |

## Default sub-command

When no explicit sub-command is passed:

- `--version` → version output
- inputs present (files, dirs, URLs, snippets) → `run`
- snippet via `-e` / `--execute-*`, or `--main-class` with `--classpath` → `run`
- otherwise → `repl`

Examples:

```bash
scala-cli a.scala          # run
scala-cli compile a.scala  # compile
scala-cli test .           # test
scala-cli repl             # REPL
```

## Common inputs

- source files (`.scala`, `.sc`, …)
- directories containing sources
- URLs pointing to sources
- piped / process-substitution source code

Inputs can be mixed.

## More

- https://scala-cli.virtuslab.org/docs/commands/run
- https://scala-cli.virtuslab.org/docs/commands/compile
- https://scala-cli.virtuslab.org/docs/commands/test
- https://scala-cli.virtuslab.org/docs/reference/commands
