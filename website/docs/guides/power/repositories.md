---
title: Repositories ⚡️
sidebar_position: 52
---

Scala CLI downloads the dependencies declared in your projects using [Coursier](https://get-coursier.io/).
The default repositories being searched are the Maven Central and local Ivy repository on your machine.
If additional repositories are required it is possible to declare them:
- on the command line with `--repository` or `--repo` or just `-r`
- with the `//> using repositories` directive

The values can be names of predefined repositories accepted by Coursier, some of which are:
- `sonatype:_value_` and `sonatype-s01:_value_` for Sonatype servers e.g. `sonatype:snapshots`
    snapshots from both servers are searched when using `snapshots`
- `jitpack`
- `m2Local`

## Custom repositories

Supplying the address of custom repositories is also accepted when using `--repository` or `//> using repositories`.
To do so, provide the URL to the repository's root, e.g. `https://maven.pkg.github.com/USER/REPO` for GitHub Package Registry.
By default, custom repositories are treated as Maven repositories, to specify an Ivy repository, prefix the address with `ivy:` and supply the ivy pattern at the end e.g. `ivy:http://localhost:8081/repository/ivy-releases/[defaultPattern]`.

:::tip
`[defaultPattern]` gets expanded by Coursier to: 
```text
    [organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]
```
:::

## Repository Authentication

:::caution
Even though the `config` command is not restricted, some available configuration keys may be, and thus may
require setting the `--power` option to be used.
That includes the configuration key tied to repositories settings, like `repositories.credentials` and others.
You can pass the `--power` option explicitly or set it globally by running:
```bash ignore
scala-cli config power true
```
:::

Repository authentication is also supported and there are a couple ways of using it:
- specifying credentials for each host in `COURSIER_CREDENTIALS` environment variable or in the `coursier.credentials` java property ([read more here](../../guides/advanced/java-properties.md)),
    the supported format in this case is `host-address username:password`, e.g. `my_domain.com MyUserName:myPasswOrd`
- adding config entries for each host, this can be done using `scala-cli --power config repositories.credentials host _username_ _password_`,
    username and password values should follow the [password option format](../../reference/password-options.md), e.g. 
```bash ignore
  scala-cli --power config repositories.credentials maven.pkg.github.com value:PrivateToken env:GH_TOKEN
```

## Default repositories

You can override the default Coursier repositories (Maven Central and local Ivy) globally by invoking:
```bash ignore
scala-cli --power config repositories.default https://first-repo.company.com https://second-repo.company.com
```

This **replaces** the built-in default repositories for all dependency resolution, including compiler
and library downloads. Repositories added via `--repository` or `//> using repository` are not
affected — those remain additive on top of the configured defaults.

To include Maven Central alongside your internal repository, list both:
```bash ignore
scala-cli --power config repositories.default https://repo1.maven.org/maven2 https://nexus.company.com/repository/maven-public
```

Alternatively, you can set default repositories via the `COURSIER_REPOSITORIES` environment variable
(pipe-separated) or the `coursier.repositories` Java property.

## Mirrors

If you're fine directly downloading artifacts from the internet, but would rather have some
repository requests go through a repository of yours, configure mirror repositories.

To redirect a specific repository URL to your mirror:
```bash ignore
scala-cli --power config repositories.mirrors https://repository.company.com/maven=https://repo1.maven.org/maven2
```

To have **all** requests to any Maven repository go through a repository of yours:
```bash ignore
scala-cli --power config repositories.mirrors maven:https://repository.company.com/maven=*
```

Mirrors apply to all Coursier-based operations including compiler downloads, dependency
resolution, and Bloop server downloads. JVM downloads (via `--jvm`) are not affected, as they
use a separate download mechanism.

### Mirror string syntax

The format is `destination=source` — the mirror target comes first, then the original repository:

| Syntax | Type | Effect |
|---|---|---|
| `<mirror-url>=<original-url>` | Maven | Redirects `<original-url>` to `<mirror-url>` |
| `maven:<mirror-url>=*` | Maven | Redirects **all** Maven repository URLs to `<mirror-url>` |
| `tree:<mirror-url>=<original-url>` | Tree | Redirects URLs starting with `<original-url>` prefix to `<mirror-url>` |

### Alternative mirror configuration

Mirrors can also be configured via:
- The `COURSIER_MIRRORS` environment variable, pointing to a `mirror.properties` file
- The `coursier.mirrors` Java property (e.g. via `scala-cli --power config java.properties -Dcoursier.mirrors=/path/to/mirror.properties`)
- A `mirror.properties` file in the Coursier configuration directory

The `mirror.properties` file format:
```properties
central.from=https://repo1.maven.org/maven2
central.to=https://repository.company.com/maven
central.type=maven
```

### Using mirrors with default repositories

If both `repositories.default` and `repositories.mirrors` are configured, default repositories
are resolved first (which repos to query), then mirrors transform the URLs (where requests
actually go). For example:
```bash ignore
scala-cli --power config repositories.default https://repo1.maven.org/maven2
scala-cli --power config repositories.mirrors maven:https://nexus.company.com/maven=*
```
This uses Maven Central as the default repository, but routes all requests through `nexus.company.com`.

## Corporate / air-gapped environments

For environments behind a firewall where Maven Central is not directly reachable:

1. **Override default repositories** to point at your internal repository:
```bash ignore
scala-cli --power config repositories.default https://nexus.company.com/repository/maven-public
```

2. Or **set up a mirror** to redirect Maven Central:
```bash ignore
scala-cli --power config repositories.mirrors maven:https://nexus.company.com/repository/maven-public=*
```

3. If your repository requires authentication, add credentials:
```bash ignore
scala-cli --power config repositories.credentials nexus.company.com value:username value:password
```