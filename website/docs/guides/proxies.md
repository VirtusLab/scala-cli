---
title: Proxies ⚡️
sidebar_position: 51
---

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

## Default repositories

If you don't rely on proxies, but rather download artifacts through different Maven repositories,
set those repositories like:
```bash ignore
scala-cli --power config repositories.default https://first-repo.company.com https://second-repo.company.com
```

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
