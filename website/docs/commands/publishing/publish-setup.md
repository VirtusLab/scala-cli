---
title: Publish Setup ⚡️
sidebar_position: 19
---

:::caution
The Publish Setup command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

:::caution
The `publish setup` sub-command is an experimental feature.

Please bear in mind that non-ideal user experience should be expected.
If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team
on [GitHub](https://github.com/VirtusLab/scala-cli).
:::

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

The `publish setup` sub-command configures your project for publishing to Maven repositories,
such as Maven Central or GitHub Packages. It checks that all required parameters for publishing are set, and tries
to infer many of them from the environment. It writes configuration as using directives, appended
to a file named `publish-conf.scala` at the root of the workspace.

`publish setup` can configure publishing, so that you can publish from your local machine,
but also from GitHub actions.
In particular, it can upload secrets as GitHub repository secrets, so that only
minimal effort is required to setup publishing from GitHub actions.

Running `publish setup` works fine on brand new machines or projects, but it works better when:
- user details, credentials for publishing, and a PGP key are configured (user-wide)
- the current project is already pushed to GitHub

## Configuration

Configuring Scala CLI for publishing makes `publish setup` works more smoothly later on.

In particular, one can configure:
- details about oneself (name, email, website - written in the developers section of POM files)
- a PGP key pair (to sign artifacts)
- Sonatype credentials (to upload artifacts to Maven Central)
- a GitHub token (to upload repository secrets to GitHub, and artifacts to Maven repositories of GitHub Packages)

User-wide configuration in Scala CLI is handled by the [`config` command](/docs/commands/config.md), and
the sections below show how to use it to configure things for `publish setup`.

:::caution
Even though the `config` command is not restricted, some available configuration keys may be, and thus may
require setting the `--power` option to be used.
That includes configuration keys tied to publishing, like `publish.user.name` and others.
You can pass the `--power` option explicitly or set it globally by running:
```bash ignore
scala-cli config power true
```
:::

### User details

Set details with
```bash
scala-cli --power config publish.user.name "Alex Me"
scala-cli --power config publish.user.email "alex@alex.me"
scala-cli --power config publish.user.url "https://alex.me"
```

The email can be left empty if you'd rather not put your email in POM files:
```bash
scala-cli --power config publish.user.email ""
```

### PGP key pair

Generate a PGP key pair for publishing with
```bash
scala-cli --power config --create-pgp-key
```

This sets 3 entries in the Scala CLI configuration, that you can print with
```bash
scala-cli --power config pgp.public-key
scala-cli --power config pgp.secret-key
scala-cli --power config pgp.secret-key-password
```

### Sonatype credentials

Publishing to Maven Central requires a Sonatype account, and requesting the right to publish
under specific organizations.
You can follow the
[sbt-ci-release Sonatype instructions](https://github.com/sbt/sbt-ci-release#sonatype)
to create an account there. Either your real Sonatype username and password, or Sonatype tokens, can be used
in Scala CLI (via the `publish.credentials` config key in both cases).

These can be written in the Scala CLI configuration the following way:
```bash ignore
scala-cli config publish.credentials s01.oss.sonatype.org env:SONATYPE_USER env:SONATYPE_PASSWORD --password-value
```

Note that both user and password arguments are assumed to be secrets, and
accept the format documented [here](/docs/reference/password-options.md). Beyond environment
variables, commands or paths to files can provide those values. They can also be passed
as is on the command line, although this is not recommended for security reasons.

In the example above, we pass the username and password via the environment, and
ask the `config` sub-command to read environment variables and persist the password values
(via `--password-value`).

If you'd rather persist the environment variable names in the Scala CLI configuration, rather than
their values, you can do
```bash ignore
scala-cli --power config publish.credentials s01.oss.sonatype.org env:SONATYPE_USER env:SONATYPE_PASSWORD
```

Note that in this case, both `SONATYPE_USER` and `SONATYPE_PASSWORD` will need to be available
in the environment when using those credentials in the `publish` sub-command.

### GitHub token

`publish setup` uses a GitHub token in order to:
- upload secrets as GitHub repository secrets
- upload artifacts to GitHub packages, when publishing to it

To setup a token for Scala CLI, you need to generate a token first.
For that, head to your [Personal access tokens page](https://github.com/settings/tokens),
and click "Generate new token". The "public_repo" scope is required to upload
repository secrets, and the "write:packages" scope is required to upload artifacts
to GitHub packages.

Once created, copy the token in your clipboard, and run
```bash ignore
# macOS
scala-cli config github.token command:pbpaste --password-value
# Linux
scala-cli config github.token "command:xclip -selection clipboard -o" --password-value
```

## Pushing project to GitHub

`publish setup` infers some publishing parameters from the GitHub URL of your project.
It also uploads repository secrets there, when setting up publishing on GitHub actions.

To create a new repository from a project, head to <https://repo.new>, pick a name
for your project and create the repository. Note its URL, and do
```bash ignore
scala-cli default-file .gitignore --write # if you don't have a .gitignore already
git init # if git isn't set up already
git remote add origin https://github.com/org/name # replace org/name with your freshly created repository values
```

## Local setup

To setup publishing in order to publish from your local machine, you can run

<ChainedSnippets>

```bash ignore
scala-cli publish setup .
```

```text
9 options need to be set

organization:
  computing io.github.scala-cli from GitHub account scala-cli
name:
  using workspace directory name hello-scala-cli
computeVersion:
  assuming versions are computed from git tags
repository:
  using Maven Central via its s01 server
license:
  using Apache-2.0 (default)
url:
  computing from GitHub repository scala-cli/hello-scala-cli
vcs:
  using GitHub repository scala-cli/hello-scala-cli
developers:
  using Alex Me <alex@alex.me> (https://github.com/scala-cli) from config

Wrote ./publish-conf.scala

Project is ready for publishing!
To publish your project, run
  scala-cli publish .
```

</ChainedSnippets>

You can then publish your project from your local machine with

<ChainedSnippets>

```bash ignore
scala-cli publish .
```

```text
Publishing io.github.scala-cli:hello-scala-cli_3:0.1.0-SNAPSHOT
 ✔ Computed 8 checksums
 🚚 Wrote 12 files

 👀 Check results at
  https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/scala-cli/hello-scala-cli_3/0.1.0-SNAPSHOT
```

</ChainedSnippets>

## GitHub actions setup

To setup publishing from GitHub actions, you can run

<ChainedSnippets>

```bash ignore
scala-cli publish setup . --ci
```

```text
11 options need to be set

organization:
  computing io.github.scala-cli from GitHub account scala-cli
name:
  using workspace directory name hello-scala-cli
computeVersion:
  assuming versions are computed from git tags
repository:
  using Maven Central via its s01 server
publish.user:
  using publish.credentials from Scala CLI configuration
publish.password:
  using publish.credentials from Scala CLI configuration
license:
  using Apache-2.0 (default)
url:
  using GitHub repository https://github.com/scala-cli/hello-scala-cli
vcs:
  using GitHub repository scala-cli/hello-scala-cli
developers:
  using Alex Me <alex@alex.me> (https://github.com/scala-cli) from config

Uploading 4 GitHub repository secrets
  updated PUBLISH_USER
  updated PUBLISH_PASSWORD
  updated PUBLISH_SECRET_KEY
  updated PUBLISH_SECRET_KEY_PASSWORD

Uploaded key 0xe58386629a30f5c5 to http://keyserver.ubuntu.com:11371

Wrote ./publish-conf.scala
Wrote workflow in ./.github/workflows/ci.yml

Commit and push ./publish-conf.scala, ./.github/workflows/ci.yml, to enable publishing from CI
```

</ChainedSnippets>

Then committing and pushing the suggested files `publish-conf.scala` and `.github/workflows/ci.yml`
should trigger a workflow pushing snapshot artifacts to Sonatype Snapshots.

To publish a non-snapshot version, either push a tag like `v0.1.0` (or any other version with a `v`
prefix), or create a release with a tag with the same name from the GitHub UI.

## GitHub Packages

In order to setup publishing to GitHub packages, pass `--publish-repository github` to the
`publish setup` commands above, like
```bash ignore
scala-cli --power publish setup . --publish-repository github
```
