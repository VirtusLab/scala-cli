---
title: Version
sidebar_position: 25
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

The `version` sub-command prints the currently used Scala CLI version and the associated Scala version.

<ChainedSnippets>

```bash
scala-cli version
```

```text
Scala CLI version: 0.1.19
Scala version (default): 3.2.1
```

</ChainedSnippets>

It is also possible to print the same output with the `-version` option passed to the default sub-command.
This way doesn't allow to use the other options relevant to `version`, however.

<ChainedSnippets>

```bash
scala-cli -version
```

```text
Scala CLI version: 0.1.19
Scala version (default): 3.2.1
```

</ChainedSnippets>

When `version` is called, Scala CLI will automatically check if it's up to date.
If your version is outdated, you will get a warning.

```text
Your Scala CLI. version is outdated. The newest version is 0.1.19
It is recommended that you update Scala CLI through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.
```

You can skip checking if Scala CLI is up to date by passing the `--offline` option.

```bash
scala-cli version --offline
```

It's also possible to just print the raw Scala CLI version with the `--cli-version` option.
This won't check if the app is outdated, so the `--offline` option is unnecessary in this context.

<ChainedSnippets>

```bash
scala-cli version --cli-version
```

```text
0.1.19
```

</ChainedSnippets>

:::note
Do not confuse the `version` sub-command's `--cli-version` option with the launcher option under the same name, as they
do different things. The former prints the raw Scala CLI version, while the latter allows to change the Scala CLI
launcher version. In fact, both of them can be used at one time.

<ChainedSnippets>

```bash
scala-cli --cli-version 0.1.18 version --cli-version
```

```text
0.1.18
```

</ChainedSnippets>

Launcher options have to be passed before the sub-command is specified, which allows to differentiate between them.
:::

Similarly, it's possible to just print the raw default Scala version.
Once more, this won't check if the app is outdated, so the `--offline` option is unnecessary in this context as well.

<ChainedSnippets>

```bash
scala-cli version --scala-version
```

```text
3.2.1
```

</ChainedSnippets>
