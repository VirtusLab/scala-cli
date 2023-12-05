---
title: HTTP Proxies ⚡️
sidebar_position: 51
---

Scala CLI can download dependencies via HTTP proxies. Proxies can be setup in several ways:
- via Java properties
- via the Maven configuration file (recommended for now)
- via Scala CLI or coursier configuration files (soon)

## Java properties
It is possible to specify the proxy settings using Java properties. There are several ways to pass those to Scala CLI, [more information here](../advanced/java-properties.md).

The most basic way is to pass the Java properties directly to Scala CLI on the command line.
Keep in mind that properties, put before the sub-command name and sources, are only passed to Scala CLI and not to the JVM executing user's code.

Example (notice the different name of the property depending on the protocol `http` or `https`):
```
$ scala-cli \
    -Dhttp.proxyProtocol=http -Dhttp.proxyHost=proxy.corp.com -Dhttp.proxyPort=8080 \
    -Dhttp.proxyUsername=alex -Dhttp.proxyPassword=1234 \
    -Dhttps.proxyProtocol=http -Dhttps.proxyHost=proxy.corp.com -Dhttps.proxyPort=8080 \
    -Dhttps.proxyUsername=alex -Dhttps.proxyPassword=1234 \
    run .
```

More information about the Java properties used for configuring proxies can be found [here](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html).

## Maven configuration file

This file lives at `~/.m2/settings.xml`

Example configuration file, without authentication:
```xml
<settings>
  <proxies>
    <proxy>
      <id>test-proxy</id>
      <protocol>http</protocol>
      <host>proxy.corp.com</host>
      <port>8080</port>
    </proxy>
  </proxies>
</settings>
```

Example configuration file, with authentication:
```xml
<settings>
  <proxies>
    <proxy>
      <id>test-proxy</id>
      <protocol>http</protocol>
      <host>proxy.corp.com</host>
      <port>8080</port>
      <username>alex</username>
      <password>1234</password>
    </proxy>
  </proxies>
</settings>
```

The value in `<protocol>…</protocol>` is assumed to be the protocol of the proxy itself
(can be either `http` or `https`, `https` is assumed by default not to inadvertently leak
proxy credentials).

Such a proxy is used for both http and https by Scala CLI.

The [coursier](https://github.com/coursier/coursier) command-line and library also pick those credentials, since version `2.1.0-M6-26-gcec901e9a` (2022/05/31).

## Scala CLI configuration files
:::caution
Even though the `config` command is not restricted, some available configuration keys may be, and thus may
require setting the `--power` option to be used.
That includes configuration keys tied to setting up proxies, like `httpProxy.address` and others.
You can pass the `--power` option explicitly or set it globally by running:
```bash ignore
scala-cli config power true
```
:::

:::warning
This way of configuring proxies is not recommended, since it will set up a proxy not only for Scala CLI, but for Coursier itself, which is used by other build tools like SBT.
This may result in unexpected behavior.

If using Scala CLI config is preferred, it's recommended to put the [relevant Java properties](#java-properties) into the `config` with:
```bash ignore
    scala-cli --power config -i java.properties "http.proxyProtocol=http" "http.proxyHost=proxy.corp.com" "http.proxyPort=8080" "https.proxyUsername=alex" "https.proxyPassword=1234"
````
The -D prefix can be dropped when writing properties to config.
:::

Scala CLI configuration can also be used to configure proxies globally.
To do that use the `config` command:

```bash ignore
scala-cli --power config httpProxy.address http://proxy.company.com:8081
```

Replace `proxy.company.com` by the address of your proxy and append the port number with `:` if needed.
Also, change `http://` to `https://` in the address if your proxy is accessible via HTTPS.

If your proxy requires authentication, set your user and password with
```bash ignore
scala-cli --power config httpProxy.user value:_encoded_user_
scala-cli --power config httpProxy.password value:_encoded_password_
```

Replace `_encoded_user_` and `_encoded_password_` by your actual user and password, following
the [password option format](/docs/reference/password-options.md). They should typically look like
`env:ENV_VAR_NAME`, `file:/path/to/file`, or `command:command to run`.
