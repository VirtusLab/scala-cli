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
$ curl -fL https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.gz | gzip -d > scala-cli
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
$ curl -fL https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-apple-darwin.gz | gzip -d > scala-cli
$ chmod +x scala-cli
$ mv scala-cli /usr/local/bin/scala-cli
```

Check that it runs fine by running its `about` command:
```text
$ scala-cli about
```

### Windows

Note that the Windows manual installation requires [Visual C++ redistributable](https://support.microsoft.com/en-us/topic/the-latest-supported-visual-c-downloads-2647da03-1eea-4433-9aff-95f26a218cc0)
to be installed. See below for how to install it.

Download the launcher from GitHub release assets with
```text
> curl -fLo scala-cli.zip https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.zip
> tar -xf scala-cli.zip
```

Check that it runs fine by running its `about` command:
```text
> scala-cli about
```

If you get an error about `MSVCR100.dll` being missing, you have to install
[Visual C++ redistributable](https://support.microsoft.com/en-us/topic/the-latest-supported-visual-c-downloads-2647da03-1eea-4433-9aff-95f26a218cc0). A valid version is distributed with the Scala CLI launchers.
You can download it [here](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/vc_redist.x64.exe),
and install it by double-clicking on it. Once the Visual C++ redistributable runtime is installed,
check that the Scala CLI runs fine by running its `about` command:
```text
> scala-cli about
```

Note that the commands above don't put the `scala-cli` command in the `PATH`. For that, you can create a directory, move the
launcher there, and add the directory to the `PATH` with
```text
> md "%USERPROFILE%/scala-cli"
> scala-cli add-path "%USERPROFILE%/scala-cli"
> move scala-cli.exe "%USERPROFILE%/scala-cli"
```

## OS-specific packages

### Debian (x86-64)

[Download Debian package](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb)

Alternatively, get and install the Debian package with
```text
$ curl -fLo scala-cli.deb https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb
$ dpkg -i scala-cli.deb
```

### RPM (x86-64)

[Download RPM package](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.rpm)

### Windows (x86-64)

[Download installer](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.msi)

### macOS (pkg)

[Download package](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-apple-darwin.pkg)

Once downloaded, right-click on `scala-cli-x86_64-apple-darwin.pkg` from Finder, and choose "Open".

### macOS (brew)

…

## Shell completions

Only bash and zsh completions are offered for now.

Try the completions with
```text
$ eval "$(scala-cli install completions --env)"
$ scala-cli --<TAB>
```

Install them on your system with
```text
$ scala-cli install completions
```

If any of the `scala-cli install completions` command complained that your shell cannot be determined, specify it
with `--shell`, like
```text
$ scala-cli install completions --shell zsh
```
