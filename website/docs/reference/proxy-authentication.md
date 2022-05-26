---
title: Proxy authentication
sidebar_position: 9
---

Scala CLI can download dependencies via HTTP proxies. Proxies can be setup in several ways:
- via the Maven configuration file (recommended for now)
- via command-line options
- via Scala CLI or coursier configuration files (soon)

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

## Command-line options

Example
```
$ scala-cli \
    -Dhttp.proxyProtocol=http -Dhttp.proxyHost=proxy.corp.com -Dhttp.proxyPort=8080 \
    -Dhttp.proxyUsername=alex -Dhttp.proxyPassword=1234 \
    -Dhttps.proxyProtocol=http -Dhttps.proxyHost=proxy.corp.com -Dhttps.proxyPort=8080 \
    -Dhttps.proxyUsername=alex -Dhttps.proxyPassword=1234 \
    <(echo 'println("Hello from Scala CLI")')
```

## Coursier or Scala CLI configuration files

Support to be added soon
