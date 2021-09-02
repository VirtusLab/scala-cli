---
title: IDE support
sidebar_position: 10
---

IDE support for sources managed by the Scala CLI is experimental, and limited to
[Metals from VSCode](https://scalameta.org/metals/docs/editors/vscode) for now.

## VSCode

### Setup

Scala CLI support in Metals / VSCode requires the latest Metals VSCode extension (>= `1.10.8`). Ensure
it is installed and up-to-date, or install or update it from the Extension panel in VSCode.

Scala CLI support relies on a custom Metals server for now. To enable it in the current project,
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
