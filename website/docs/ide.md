---
title: IDE support
sidebar_position: 10
---

IDE support for sources managed by the Scala CLI is experimental, and limited to
[Metals from VSCode](https://scalameta.org/metals/docs/editors/vscode) for now.

## VSCode

### Setup

Scala CLI support in Metals / VScode requires a custom version of the Metals VSCode extension.
Use the following commands to install it:
```text
$ curl -fLo metals-scala-cli.vsix https://github.com/alexarchambault/metals-vscode/raw/scala-cli/metals-1.10.7.vsix
$ code --install-extension metals-scala-cli.vsix
```

Scala CLI support relies on a custom Metals server for now. To enable it in the current project,
run the command "Create New Integrated Terminal (in Active Workspace)", and type
```text
$ mkdir -p .vscode
$ cat > .vscode/settings.json << EOF
{
  "metals.serverVersion": "com.github.alexarchambault.tmp.metals:metals_2.12:0.10.4+193-06810ef8-SNAPSHOT",
  "metals.serverProperties": [
    "-Xmx512m",
    "-Dmetals.scala-cli.launcher=$(which scala)"
  ]
}
EOF
```

A window reload should be needed for this change to be taken into account. If Metals doesn't
suggest it, run the "Developer: Reload window" command from the command palette.

### Activating the Scala CLI support

Scala CLI support needs to be manually activated for specific files. Open an existing `.scala`
or `.sc` file,
or create a new one. Then, with that file opened and focused, run the command
"Metals: Start Scala CLI server"


- enjoy
  - go-to-source
  - accurate diagnostics
  - find usages
  - …

- automatic re-import when adding / removing dependencies
