---
title: Repositories and HTTP Proxies ⚡️
sidebar_position: 51
---

Scala CLI downloads the dependencies declared in your projects using Coursier.
The default repositories being searched are the Maven Central and local Ivy repository on your machine.
If additional repositories are required it is possible to declare them:
- on the command line with `--repository` or `--repo` or just `-r`
- with the `//> using repositories` directive

The values accepted above can be a predefined repository accepted by Coursier, some of which are:
- `sonatype:_value_` and `sonatype-s01:_value_` for Sonatype servers e.g. `sonatype:snapshots`
    snapshots from both servers are searched when using `snapshots`
- `jitpack`
- `m2Local`

## Custom repositories

Supplying the address of custom repositories is also accepted when using `--repository` or `//> using repositories`.
To do so provide the URL to the repository's root, e.g. `https://maven.pkg.github.com/USER/REPO`.
By default, custom repositories are treated as Maven repositories, to specify an Ivy repository, prefix the address with `ivy:`, e.g. `ivy:http://localhost:8081/repository/ivy-releases/[defaultPattern]`.
Remember to supply the ivy pattern, `[defaultPattern]` expands to `[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]`

Repository authentication is supported with credential interpolation into the repository address.
To use it, insert `{USER_NAME}:{PASSWORD}@` or `{TOKEN}@` (if the username is not required) before the domain name, values in the brackets are interpreted according to the following rules:
- `env:ENV_NAME` will read the value of the environment variable ENV_NAME
- `file:/path/tofile/cred.txt` will read the contents of the specified file, path can be relative

E.g.:
- `ivy:https://{env:USER_NAME}:{file:/keys/password.txt}@localhost:8081/repository/ivy-releases/[defaultPattern]`
- `https://{env:GH_TOKEN}@maven.pkg.github.com/USER/REPO`

## Default repositories

You can set the default Coursier repositories globally by invoking:
```bash ignore
scala-cli --power config repositories.default https://first-repo.company.com https://second-repo.company.com
```

## HTTP proxies

:::caution
Even though the `config` command is not restricted, some available configuration keys may be, and thus may
require setting the `--power` option to be used.
That includes configuration keys tied to setting up proxies, like `httpProxy.address` and others.
You can pass the `--power` option explicitly or set it globally by running:
```bash ignore
scala-cli config power true
```
:::

### Configuration

If you can only download artifacts through a proxy, you need to configure it beforehand, like
```bash ignore
scala-cli --power config httpProxy.address http://proxy.company.com
```

Replace `proxy.company.com` by the address of your proxy.

Change `http://` to `https://` if your proxy is accessible via HTTPS.

### Authentication

If your proxy requires authentication, set your user and password with
```bash ignore
scala-cli --power config httpProxy.user value:_encoded_user_
scala-cli --power config httpProxy.password value:_encoded_password_
```

Replace `_encoded_user_` and `_encoded_password_` by your actual user and password, following
the [password option format](/docs/reference/password-options.md). They should typically look like
`env:ENV_VAR_NAME`, `file:/path/to/file`, or `command:command to run`.

## Mirrors

If you're fine directly downloading artifacts from the internet, but would rather have some
repositories requests go through a repository of yours, configure mirror repositories, like
```bash ignore
scala-cli --power config repositories.mirrors https://repo1.maven.org/maven2=https://repository.company.com/maven
```

To have all requests to a Maven repository go through a repository of yours, do
```bash ignore
scala-cli --power config repositories.mirrors maven:*=https://repository.company.com/maven
```