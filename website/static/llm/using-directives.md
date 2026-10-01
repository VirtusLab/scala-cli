# Using directives

`using` directives define configuration inside `.scala` / `.sc` source files so you often need no separate build file.

Human docs: https://scala-cli.virtuslab.org/docs/guides/introduction/using-directives  
Full reference: https://scala-cli.virtuslab.org/docs/reference/directives

## Syntax

Directives live in special comments **before any other Scala code**:

```scala
//> using scala 3.3.3
//> using dep com.lihaoyi::os-lib:0.11.3
//> using options -Xasync
```

CLI flags override directive values.

## Scope

Most directives apply to the whole compilation scope of the app or test sources. `using target` directives apply only to the file that declares them (experimental).

## Common directives

```scala
//> using scala 3.3.3
//> using dep org.typelevel::cats-core:2.12.0
//> using test.dep org.scalameta::munit::1.0.0
//> using options -deprecation -feature
//> using jvm 17
//> using platform scala-js
```

## Deprecated forms

Older forms (`@using`, `// using`, bare `using`) from early `0.0.x` experiments are deprecated and ignored from `1.0.x`. Prefer `//> using …`.
