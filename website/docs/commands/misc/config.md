---
title: Config
sidebar_position: 1
---

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

The `config` sub-command makes it possible to get and set various configuration values, used by
other Scala CLI sub-commands.

Examples of use:
<ChainedSnippets>

```bash
scala-cli config publish.user.name "Alex Me"
scala-cli config publish.user.name
```

```text
Alex Me
```

</ChainedSnippets>

The `--dump` option allows to print all config entries in JSON format:
<ChainedSnippets>

```bash
scala-cli config --dump | jq .
```
```json
{
  "github": {
    "token": "value:qWeRtYuIoP"
  },
  "pgp": {
    "public-key": "value:-----BEGIN PGP PUBLIC KEY BLOCK-----\nVersion: BCPG v1.68\n\n…\n-----END PGP PUBLIC KEY BLOCK-----\n",
    "secret-key": "value:…",
    "secret-key-password": "value:1234"
  },
  "user": {
    "email": "alex@alex.me",
    "name": "Alex Me",
    "url": "https://alex.me"
  }
}
```

</ChainedSnippets>

Use `--password` to get the value of a password entry:

<ChainedSnippets>

```bash
export MY_GITHUB_TOKEN=1234
scala-cli config github.token "env:MY_GITHUB_TOKEN"
scala-cli config github.token
```
```text
env:MY_GITHUB_TOKEN
```
```bash
export MY_GITHUB_TOKEN=1234
scala-cli --power config --password github.token
```
```text
1234
```

</ChainedSnippets>

Use `--create-key` to create a PGP key pair, protected by a randomly-generated password, to
be used by the `publish setup` sub-command:
```sh
scala-cli config --create-key
```

Configuration values are stored in a directory under your home directory, with restricted permissions:
- on macOS: `~/Library/Application Support/ScalaCli/secrets/config.json`
- on Linux: `~/.config/scala-cli/secrets/config.json`
- on Windows: `%LOCALAPPDATA%\ScalaCli\secrets\config.json` (typically `C:\Users\username\AppData\Local\ScalaCli\secrets\config.json`)
