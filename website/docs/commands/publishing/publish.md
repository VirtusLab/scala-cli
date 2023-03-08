---
title: Publish ⚡️
sidebar_position: 20
---

:::caution
The Publish command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

:::caution
The `publish` sub-command is an experimental feature.

Please bear in mind that non-ideal user experience should be expected.
If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team
on [GitHub](https://github.com/VirtusLab/scala-cli).
:::

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

The `publish` sub-command allows to publish Scala CLI projects to Maven repositories.

We recommend running [the `publish setup` sub-command](./publish-setup.md) once prior to
running `publish`, in order to set missing `using` directives for publishing, but this is not
mandatory.

## Required settings

`scala-cli publish` and `scala-cli publish local` might complain about missing settings.
An organization, a name (or a module name), and (a way to compute) a version are needed, but Scala CLI may
be able to compute sensible defaults for them.

We recommend setting the settings below via using directives rather than on the command-line,
so that commands such as `scala publish .` or `scala publish local .` work fine for your
project. Command-line options for those settings take over the using directive values, and are
provided as a convenience.

This table lists settings allowing to specify those. See the sub-sections right after for more details.

|          | `using` directive | Command-line option | Example values | Notes |
|----------|-------------------|---------------------|----------------|-------|
| Organization | `publish.organization` | `--organization` | `org.virtuslab.scala-cli` | |
| Name | `publish.name` | `--name` | `scala-cli` | |
| Module Name | `publish.moduleName` | `--module-name` | `scala-cli_3` | Module Name includes the Scala prefix, such as `_2.13` or `_3`. Specifying Name should be favored over Module Name |
| Compute Version | `publish.computeVersion` | `--compute-version` | `git:tag` | |
| Version | `publish.version` | `--version` | `0.1.0`, `0.1.1-SNAPSHOT` | As much as possible, Compute Version (describing how to compute the version) should be favored over Version |

### Organization

If your Scala CLI project lives in a git repository having a GitHub remote, Scala CLI
will infer an organization from it: if your project lives in GitHub organization `foo`
(that is, lives somewhere under `https://github.com/foo/`), Scala CLI will use
`io.github.foo` as default Maven organization.

To override this default value, set the `publish.organization` directive, like
```scala
//> using publish.organization "io.github.foo"
```

### Name

Scala CLI will use the project directory name as default Maven name. That is, if your
Scala CLI project lives in a directory named `something`, it will be published as
`something` (pure Java project) or `something_3` (Scala 3 project) for example.

To override this default value, set the `publish.name` directive, like
```scala
//> using publish.name "something"
```

### Version

If your Scala CLI project lives in a git repository, Scala CLI will infer a way to compute
versions from it: if the current commit has a tag `v1.2.3`, version `1.2.3` is assumed.
Else, if it has such a tag earlier in the git history, version `1.2.4-SNAPSHOT` is assumed.

To override this default value, set the `publish.computeVersion` directive, like
```scala
//> using publish.computeVersion "git:tag"
```

## Repository settings

A repository is required for the `publish` command, and might need other settings to work fine
(to pass credentials for example). See [Repositories](#repositories) for more information.

When publishing from you CI, we recommend letting `scala-cli publish setup`
setting those settings via using directives. When publishing from your local machine to Maven Central,
we recommend setting the repository via a `publish.repository` directive, and keeping your
Sonatype credentials in the Scala CLI settings, via commands such as
```bash
scala-cli config publish.credentials s01.oss.sonatype.org env:SONATYPE_USER env:SONATYPE_PASSWORD
```

<!-- TODO Automatically generate that? -->
|          | `using` directive | Command-line option | Example values | Notes |
|----------|-------------------|---------------------|----------------|-------|
| Repository | `publish.repository` | `--publish-repository` | `central`, `central-s01`, `github`, `https://artifacts.company.com/maven` | |

## Other settings

A number of metadata can be set either by `using` directives, or from the command-line. These
metadata are optional in the `publish local` command, but might be mandatory for some repositories
in the `publish` command, like Maven Central for non-snapshot versions.

We recommend setting the settings below via using directives rather than on the command-line,
so that these don't have to be recalled for each `scala-cli publish` or `scala-cli publish local`
invocation. Command-line options for those settings take over the using directive values, and are
provided as a convenience.

<!-- TODO Automatically generate that? -->
|          | `using` directive | Command-line option | Example values | Notes |
|----------|-------------------|---------------------|----------------|-------|
| License | `publish.license` | `--license` | `Apache-2.0`, `MIT`, `Foo:https://foo.com/license.txt`, … | Run `scala-cli publish --license list` to list pre-defined licenses |
| URL | `publish.url` | `--url` | | |
| VCS | `publish.vcs` | `--vcs` | `github:VirtusLab/scala-cli`, `https://github.com/VirtusLab/scala-cli.git` |scm:git:github.com/VirtusLab/scala-cli.git|scm:git:git@github.com:VirtusLab/scala-cli.git` | |
| Developers | `publish.developer` | `--developer` | <code>alexme&vert;Alex Me&vert;https://alex.me</code> | Can be specified multiple times, using directives and CLI values add up |

### Signing

Scala CLI can sign the artifacts it publishes with PGP signatures. Signing in Scala CLI can be
handled by either
- the [Bouncy Castle library](https://www.bouncycastle.org) (default, recommended)
- the local `gpg` binary on your machine

A signing mechanism will be chosen based on options and directives specified,
it can also be overriden with `--signer` with one of those values: `bc`, `gpg` or `none`.

#### Bouncy Castle

A benefit of using Bouncy Castle to sign artifacts is that it has no external dependencies.
Scala CLI is able to sign things with Bouncy Castle without further setup on your side.

To enable signing with Bouncy Castle (recommended), pass a secret key with
`--secret-key file:/path/to/secret-key`. If the key is protected by a password,
pass it like `--secret-key-password env:MY_KEY_PASSWORD`.

Scala CLI can generate and keep a secret key for you. For that, create the key with
```sh
scala-cli --power config --create-gpg-key
```

This generated key kept in config will be used by default unless specified otherwise, e.g.:
- with directives:
    ```bash
    //> using publish.secretKey env:PGP_SECRET
    //> using publish.secretKeyPassword command:get_my_password
    ```

- with options:
    ```sh
    scala-cli publish \
      --secret-key env:PGP_SECRET \
      --secret-key-password file:pgp_password.txt \
      …
    ```

Since these values should be kept secret the options and directives accept the format documented [here](/docs/reference/password-options.md).
Furthermore command line options accept an extra format - `config:…` for using config entries.

#### GPG

Using GPG to sign artifacts requires the `gpg` binary to be installed on your system.
A benefit of using `gpg` to sign artifacts over Bouncy Castle is: you can use keys from
your GPG key ring, or from external devices that GPG may support.

To enable signing with GPG, pass `--gpg-key *key_id*` on the command line
or specify it by `//>using publish.gpgKey "key_id"`.
If needed, you can specify arguments meant to be passed to `gpg`,
with `--gpg-option` or `//>using publish.gpgOptions "--opt1" "--opt2"`, like
```text
--gpg-key 1234567890ABCDEF --gpg-option --foo --gpg-option --bar
```

### Checksums

Scala CLI can generate checksums of the artifacts it publishes.

By default, Scala CLI generates SHA-1 and MD5 checksums. To disable checksums,
pass `--checksum none`. To generate checksum formats to generate, pass them via
`--checksum`, separating the checksum values with `,` or using `--checksum` multiple
times:
```text
--checksum sha1,md5
--checksum sha1 --checksum md5
```

To list supported checksum types, pass `--checksum list`.

### CI overrides

Scala CLI allows some publishing-related settings to have different values on your local machine and
on CIs. In particular, this can be convenient to handle credentials and signing parameters, as these can
be read from different locations on developers' machines and on CIs.

On CIs (when `CI` is set in the environment, whatever its value), the CI override is
used if it's there. Else the main directive is used.

<!-- TODO Automatically generate that? -->
| Settings | Directive | CI override directive |
|----------|-------------------|-----------------------|
| Compute Version | `publish.computeVersion` | `publish.ci.computeVersion` |
| Repository | `publish.repository` | `publish.ci.repository` |
| Repository User | `publish.user` | `publish.ci.user` |
| Repository Password | `publish.password` | `publish.ci.password` |
| Repository Realm | `publish.realm` | `publish.ci.realm` |
| Secret Key | `publish.secretKey` | `publish.ci.secretKey` |
| Secret Key Password | `publish.secretKeyPassword` | `publish.ci.secretKeyPassword` |
| GPG key | `publish.gpgKey` | `publish.ci.gpgKey` |
| GPG options | `publish.gpgOptions` | `publish.ci.gpgOptions` |

## Repositories

### Maven Central

The easiest way right now to publish to Maven Central Repository is to use
Sonatype repositories - `s01.oss.sonatype.org` or `oss.sonatype.org`
Since 25.02.2021 `s01` is the default server for new users, if your account is older than that
you probably need to use the legacy `oss.sonatype.org`. More about this [here](https://central.sonatype.org/news/20210223_new-users-on-s01/#question).

Use `central` as repository to push artifacts to Maven Central via `oss.sonatype.org`.
To push to it via `s01.oss.sonatype.org`, use `central-s01`.

When using `central` or `central-s01` as repository, artifacts are pushed
either to `https://oss.sonatype.org/content/repositories/snapshots` (versions
ending in `SNAPSHOT`) or to `https://oss.sonatype.org/staging/deploy/maven2`
(in that case, Sonatype API endpoints are called to "close" and "release"
artifacts, which later syncs them to `https://repo1.maven.org/maven2`).

### GitHub Packages

Use `github` (GitHub organization and name computed from the git remotes)
or `github:org/name` (replace `org` and `name` by the GitHub organization and name
of your repository, like `github:VirtusLab/scala-cli`)
to push artifacts to GitHub Packages.

Note that, as of writing this, this disables parallel uploading of artifacts,
checksums, and signing (all not supported by GitHub Packages as of writing this).

### Ivy2 Local

Use `ivy2Local` to put artifacts in the local Ivy2 repository, just like how
[`publish local`](./publish-local.md) does.

### Other pre-defined repositories

All pre-defined repositories accepted by coursier, such as `jitpack` or `sonatype:snapshots`, are accepted as repositories for publishing.

### Generic Maven repositories

Pass a URL (beginning with `http://` or `https://`) to push to custom
HTTP servers. Pushing to such repositories relies on HTTP PUT requests
(just like for the pre-defined repositories above).

You can also pass a path to a local directory, absolute (recommended)
or relative (beware of name clashes with pre-defined repositories above).

### Authentication

Specify publish repository authentication either on the command-line or via
using directives. See user / password / realm in the [settings table](#settings)
and the [CI overrides](#ci-overrides).

## Publishing

Once all the necessary settings are set, publish a Scala CLI project with a command
such as this one:

<ChainedSnippets>

```sh
scala-cli publish .
```
(`.` is for the Scala CLI project in the current directory)

```text
Publishing io.github.scala-cli:hello-scala-cli_3:0.1.0-SNAPSHOT
 ✔ Computed 8 checksums
 🚚 Wrote 12 files

 👀 Check results at
  https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/scala-cli/hello-scala-cli_3/0.1.0-SNAPSHOT
```

</ChainedSnippets>
