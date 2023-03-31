---
title: Config
sidebar_position: 17
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

The `config` sub-command makes it possible to get and set various configuration values, used by
other Scala CLI sub-commands.

The full list of the available configuration keys is available in [the reference docs](../reference/commands.md#config).

Examples of use:
<ChainedSnippets>

```bash ignore
scala-cli config power true
scala-cli config power
```

```text
true
```

</ChainedSnippets>

:::caution
Even though the `config` command is not restricted, some available configuration keys may be, and thus may
require setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:
```bash ignore
scala-cli config power true
```
:::

<ChainedSnippets>

```bash
scala-cli --power config publish.user.name "Alex Me"
scala-cli --power config publish.user.name
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

Use `--password-value` to get the value of a password entry:

<ChainedSnippets>

```bash
export MY_GITHUB_TOKEN=1234
scala-cli --power config github.token "env:MY_GITHUB_TOKEN"
scala-cli --power config github.token
```

```text
env:MY_GITHUB_TOKEN
```

```bash
export MY_GITHUB_TOKEN=1234
scala-cli --power config --password-value github.token
```

```text
1234
```

</ChainedSnippets>

Use `--create-pgp-key` to create a PGP key pair, protected by a randomly-generated password, to
be used by the `publish setup` sub-command:

```sh
scala-cli --power config --create-pgp-key --pgp-password MY_CHOSEN_PASSWORD --email "some_email"
```

It's not mandatory, although recomended, to use a password to encrypt your keychains.
To store the private keychain in an unencrypted form use `--pgp-password none`, use
`--pgp-password random` for Scala CLI to randomly generate a password for you.
Also, the `--email` option or `publish.user.email` has to be specified for this subcommand to work properly.

Configuration values are stored in a directory under your home directory, with restricted permissions:

- on macOS: `~/Library/Application Support/ScalaCli/secrets/config.json`
- on Linux: `~/.config/scala-cli/secrets/config.json`
- on Windows: `%LOCALAPPDATA%\ScalaCli\secrets\config.json` (
  typically `C:\Users\username\AppData\Local\ScalaCli\secrets\config.json`)
