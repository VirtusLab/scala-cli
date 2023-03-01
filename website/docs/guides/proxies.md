---
title: Proxies ⚡️
sidebar_position: 51
---

## HTTP proxies

### Configuration

If you can only download artifacts through a proxy, you need to configure it beforehand, like
```text
scala-cli config httpProxy.address http://proxy.company.com
```

Replace `proxy.company.com` by the address of your proxy.

Change `http://` to `https://` if your proxy is accessible via HTTPS.

### Authentication

If your proxy requires authentication, set your user and password with
```text
scala-cli config httpProxy.user _encoded_user_
scala-cli config httpProxy.password _encoded_password_
```

Replace `_encoded_user_` and `_encoded_password_` by your actual user and password, following
the [password option format](/docs/reference/password-options.md). They should typically look like
`env:ENV_VAR_NAME`, `file:/path/to/file`, or `command:command to run`.

## Default repositories

If you don't rely on proxies, but rather download artifacts through different Maven repositories,
set those repositories like:
```text
scala-cli config repositories.default https://first-repo.company.com https://second-repo.company.com
```

## Mirrors

If you're fine directly downloading artifacts from the internet, but would rather have some
repositories requests go through a repository of yours, configure mirror repositories, like
```text
scala-cli config repositories.mirrors https://repo1.maven.org/maven2=https://repository.company.com/maven
```

To have all requests to a Maven repository go through a repository of yours, do
```text
scala-cli config repositories.mirrors maven:*=https://repository.company.com/maven
```
