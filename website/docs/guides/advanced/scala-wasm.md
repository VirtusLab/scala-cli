---
title: Scala.js with Wasm
sidebar_position: 41
---

:::caution
Wasm support is **experimental**. Flags, directives and behavior may change.
:::

Scala CLI can compile and run Scala.js sources through the experimental
[Scala.js WebAssembly backend](https://www.scala-js.org/doc/project/webassembly.html).
The backend compiles your code to a `.wasm` binary (plus a small `.js` loader) that runs on
JavaScript hosts embedding a recent V8 (Node.js, Deno) or JavaScriptCore (Bun) engine.

The backend needs an engine with Wasm 3.0, WasmGC, the `exnref`-based exception-handling proposal,
and ES module support. Per the Scala.js docs, that means **Node.js 23+**, Chrome 137+, Firefox 131+
or Safari 18.4+ (Bun tracks a recent JavaScriptCore).

## Enabling Wasm

Pass `--js-emit-wasm` together with `--js-module-kind es` (the Scala.js Wasm backend only emits ES
modules), or use the `//> using wasm` and `//> using jsModuleKind es` directives:

```bash ignore
scala-cli run Hello.scala --js-emit-wasm --js-module-kind es
```

```scala title=Hello.scala
//> using wasm
//> using jsModuleKind es

object Hello {
  def main(args: Array[String]): Unit =
    println("Hello from Wasm!")
}
```

Enabling Wasm implies the **JS platform**, and reports an error if you also request a conflicting
platform, e.g. `--js-emit-wasm --platform native`. The Wasm backend requires **ES module** output:
rather than overriding your settings silently, Scala CLI asks you to set `--js-module-kind es`
explicitly and fails fast if it's missing.

## Choosing a runtime

The output runs on Node.js by default. Select another runtime with `--js-runtime`
(or the `//> using jsRuntime` directive):

| Runtime | Flag                          | Engine          |
|---------|-------------------------------|-----------------|
| Node.js | `--js-runtime node` (default) | V8              |
| Deno    | `--js-runtime deno`           | V8              |
| Bun     | `--js-runtime bun`            | JavaScriptCore  |

```bash ignore
scala-cli run Hello.scala --js-emit-wasm --js-module-kind es --js-runtime deno
```

## Runtime setup

Scala CLI does **not** pass any V8 flags to the runtime — it runs your program with the runtime as-is.
Recent runtimes need no flags: Node.js 24+, Deno 2.8+ and Bun all enable the `exnref`-based exception
handling (plus WasmGC and typed function-references) by default.

Only **older** V8 still gates `exnref` behind `--experimental-wasm-exnref`. If your runtime is old
enough to require it, pass it yourself via the runtime's own environment variable:

- **Node.js**: `NODE_OPTIONS=--experimental-wasm-exnref scala-cli run … --js-emit-wasm --js-module-kind es`
- **Deno**: `DENO_V8_FLAGS=--experimental-wasm-exnref scala-cli run … --js-emit-wasm --js-module-kind es --js-runtime deno`
- **Bun**: nothing is needed; it relies on JavaScriptCore's built-in support.

The same mechanism gives you any **optional** feature flags — Scala CLI never injects these either:

- `--experimental-wasm-jspi` — required to use `js.async` / `js.await`.
- `--experimental-wasm-imported-strings` — improves performance.
- `--turboshaft-wasm` — improves stability on Node.js 23.x (Node.js 24+ enables it by default and
  has removed the flag).

```bash ignore
NODE_OPTIONS=--experimental-wasm-jspi scala-cli run Hello.scala --js-emit-wasm --js-module-kind es
```

## Limitations

These come from the Scala.js Wasm backend itself, not from Scala CLI:

- `@JSExport` / `@JSExportAll` are **silently ignored**. JavaScript therefore can't call exported
  methods of Scala classes — including `toString()`, so converting a Scala instance to a string
  *from JavaScript* (e.g. in JS string concatenation) won't work. String operations on the Scala
  side are unaffected.
- A single module only (`ModuleSplitStyle.FewestModules`); features that force multiple modules
  (several `@JSExportTopLevel` module names, `js.dynamicImport`) aren't supported yet.
- The output runs only on JavaScript-hosted engines (browsers, Node.js, Deno, Bun, Cloudflare
  Workers). Standalone Wasm VMs such as wasmtime and WasmEdge are not yet supported — see
  [scala-js/scala-js#4991](https://github.com/scala-js/scala-js/issues/4991).
