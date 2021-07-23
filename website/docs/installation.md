---
title: Installation
sidebar_position: 2
---

`scala-cli` can be installed either manually, or using OS-specific packages.
OS-specific packages are experimental and in-progress.
Manual installation is recommended for now.

## Manual installation

### Linux

Download the launcher from GitHub release assets with
```text
$ curl -fL https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-x86_64-pc-linux.gz | gzip -d > scala-cli
$ chmod +x scala-cli
$ sudo mv scala-cli /usr/local/bin/scala-cli
```

Check that it runs fine by running its `about` command:
```text
$ scala-cli about
```

### macOS

Download the launcher from GitHub release assets with
```text
$ curl -fL https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-x86_64-apple-darwin.gz | gzip -d > scala-cli
$ chmod +x scala-cli
$ sudo mv scala-cli /usr/local/bin/scala-cli
```

Check that it runs fine by running its `about` command:
```text
$ scala-cli about
```

### Windows

Download the launcher from GitHub release assets with
```text
> curl -fLo scala-cli.zip https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-x86_64-pc-win32.zip
> tar -xf scala-cli.zip
```

Check that it runs fine by running its `about` command:
```text
> scala-cli about
```

Note that this doesn't put the `scala-cli` command in the `PATH`. For that, you can create a directory, move the
launcher there, and add the directory to the `PATH` with
```text
> md "%USERPROFILE%/scala-cli"
> scala-cli add-path "%USERPROFILE%/scala-cli"
> move scala-cli.exe "%USERPROFILE%/scala-cli"
```

## OS-specific packages

### Debian (x86-64)

[Download Debian package](https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-cli.deb)

Alternatively, get and install the Debian package with
```text
$ curl -fLo scala-cli.deb https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-cli.deb
$ dpkg -i scala-cli.deb
```

### RPM (x86-64)

[Download RPM package](https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-cli.rpm)

### Windows (x86-64)

[Download installer](https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-cli.msi)

### macOS (pkg)

[Download package](https://github.com/VirtuslabRnD/scala-cli/releases/download/latest/scala-cli.pkg)

Once downloaded, right-click on scala-cli.pkg from Finder, and choose "Open".

### macOS (brew)

…

## Shell completions

### bash

### zsh
