---
title: PGP ⚡️
sidebar_position: 18
---

:::caution
The PGP command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

The `pgp` sub-commands are low-level commands, exposing the PGP capabilities of
Scala CLI. These capabilities are used in the `publish` and `publish setup` commands
in particular.

These commands make it possible to
- create PGP keys with `pgp create`
- get a key fingerprint with `pgp key-id`
- push them to / pull them from key servers with `pgp push` / `pgp pull`
- sign files with `pgp sign`
- verify signatures with `pgp verify`

These capabilities rely on the [Bouncy Castle library](https://www.bouncycastle.org).
Note that sub-commands relying on signing, such as `publish`, also allow signing
to be handled using `gpg`.

## Create key pairs

```text
$ scala-cli pgp create --email alex@alex.me --password env:MY_PASSWORD
Wrote public key e259e7e8a23475b3 to key.pub
Wrote secret key to key.skr
```

See [the dedicated page](docs/reference/password-options.md) for the various formats
accepted by the `--password` option.

## Get the fingerprint of a public key

```text
$ scala-cli pgp key-id ./key.pub
e259e7e8a23475b3
```

## Push public keys to key servers

```text
$ scala-cli pgp push key.pub
Key 0xe259e7e8a23475b3 uploaded to http://keyserver.ubuntu.com:11371
```

## Pull public keys from key servers

```text
$ scala-cli pgp pull 0x914d298df8fa4d20
-----BEGIN PGP PUBLIC KEY BLOCK-----
…
-----END PGP PUBLIC KEY BLOCK-----
```

## Sign files

```text
$ scala-cli pgp sign --secret-key file:./key.skr --password value:1234 ./foo
$ cat ./foo.asc
-----BEGIN PGP MESSAGE-----
…
-----END PGP MESSAGE-----

$ scala-cli pgp sign --secret-key file:./key.skr --password value:1234 ./foo --stdout
-----BEGIN PGP MESSAGE-----
…
-----END PGP MESSAGE-----
```

## Verify signatures

```text
$ scala-cli pgp verify --key key.pub foo.asc
foo.asc: valid signature
```
