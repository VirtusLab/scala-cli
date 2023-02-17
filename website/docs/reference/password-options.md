---
title: Password options ⚡️
sidebar_position: 8
---

Some Scala CLI options expect password / secret values. Passing passwords directly on the command-line
poses security issues, so Scala CLI offers a few ways to work around that.
Passwords / secrets can be passed: via environment variables, via a command printing the secret, via a file, or (not recommended)
inline.

## Environment variable

Prefix the environment variable name with `env:`, like
```text
$ export MY_PASSWORD=1234
$ scala-cli publish . --repo-password env:MY_PASSWORD
```

## Command printing the secret

Prefix the command printing the secret with `command:`, like
```text
$ get-secret sonatype-s01 # command printing the secret
1234
$ scala-cli publish . --repo-password "command:get-secret sonatype-s01"
```

Alternatively, if some of the command arguments contain spaces, one can pass a JSON list:
```text
$ get-secret "sonatype s01" # command printing the secret
1234
$ scala-cli publish . --repo-password 'command:["get-secret", "sonatype s01"]'
```

## File

Prefix the file path with `file:`, like
```text
$ cat "$HOME/.passwords/sonatype-s01"
1234
$ scala-cli publish . --repo-password "file:$HOME/.passwords/sonatype-s01"
```

## Inline

This is the less secure way of passing secrets to Scala CLI, and should only be used
for debugging purposes, with non-sensitive secrets. Prefix the password / secret value
with `value:`, like
```text
$ scala-cli publish . --repo-password value:1234
```
