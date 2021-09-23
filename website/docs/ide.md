---
title: IDE support
sidebar_position: 10
---

IDE support for sources managed by the Scala CLI is experimental, and limited to
[Metals](https://scalameta.org/metals/) with [VS
Code](https://scalameta.org/metals/docs/editors/vscode) or
[Neovim](https://github.com/scalameta/nvim-metals) for now.

Two kind of support co-exist for VSCode:
- [one relying purely on BSP](#vscode-bsp), with no automatic reload upon adding new dependencies for now.
- [one relying on a custom Metals server](#vscode-custom-metals-server), featuring an import right from VSCode, and having automatic reload support upon adding new dependencies,

## VSCode (BSP)

This Scala CLI support should work with any recent version of Metals. In order to
open a Scala CLI project in Metals, run the `setup-ide` command first:
```text
scala-cli setup-ide .
```
(replace `.` with another set of inputs if these are different.) This should create
a file named `.bsp/scala-cli.json`.

Then open the directory where you ran the `setup-ide` command in VSCode. Provided
the current directory doesn't also contain an sbt or Mill project, the Scala CLI
project should be automatically detected by Metals, and most Metals features (code
navigation, diagnostics in editor, etc.) should work.

If you add dependencies (via `import $ivy` or `using` directives), re-run the
`setup-ide` above, and run the command "Metals: Connect to build server".

## VSCode (custom Metals server)

### Setup

This Scala CLI support in Metals / VSCode requires the latest Metals VSCode extension (>= `1.10.8`). Ensure
it is installed and up-to-date, or install or update it from the Extension panel in VSCode.

This Scala CLI support relies on a custom Metals server for now. To enable it in the current project,
run the command "Create New Integrated Terminal (in Active Workspace)", and type
```bash
mkdir -p .vscode
cat > .vscode/settings.json << EOF
{
  "metals.serverVersion": "org.virtuslab:metals_2.12:0.10.5+65-f2a9927c-SNAPSHOT",
  "metals.serverProperties": [
    "-Xmx512m",
    "-Dmetals.scala-cli.launcher=$(which scala-cli)"
  ]
}
EOF
```

A window reload should be needed for this change to be taken into account. If Metals doesn't
suggest it, run the "Developer: Reload window" command from the command palette.

### Activating the Scala CLI support

In order for Metals to assume a `.scala` or `.sc` file is handled by the Scala CLI,
a `scala.conf` file or a file ending in `.scala.conf` needs to exist in the same
directory as the `.scala` or `.sc` files, or in a parent directory of theirs. Beware
that it needs to be in the Metals workspace though (so you can't put it at the root
of your filesystem, for example). This file can be empty.

Upon opening a `.scala` or `.sc` file while `scala.conf` or a `*.scala.conf` file exists,
Metals should open a dialog offering to:
- Import Scala CLI projects automatically
- Import
- Dismiss

Pick any of the first two options, and enjoy IDE support for your Scala CLI-managed sources!

The following Metals features are expected to work, among others:
- go-to-source
- diagnostics
- find usages

Upon adding new dependencies, via `scala.conf` or via `import $dep` in Scala sources, the
new dependencies should be automatically downloaded and be available right after in Metals.

## Neovim

### Setup

You can get Scala CLI support in Neovim by using
[`nvim-metals`](https://github.com/scalameta/nvim-metals). Scala CLI support
relies on a custom Metals server for now. To enable it make sure to set the
`g:metals_server_org` to `org.virtuslab`, and update the
`g:metals_server_version` to the desired version.

You'll also need to set a server property to detect the location of `scala-cli`.
You can set this in your settings table like so:

```lua
Metals_config = require("metals").bare_config

Metals_config.settings = {
  serverProperties = {
    "-Dmetals.scala-cli.launcher=<location of your installed scala-cli>"
  }
}
```

After updating these values make sure to run a `:MetalsInstall` command to
install the custom Metals server. You'll then either need to restart the server
with a `:MetalsRestartServer` or just close the project and reopen.
