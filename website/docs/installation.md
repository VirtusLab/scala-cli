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

### Alpine 

Download the launcher from GitHub release assets with

```text
$ wget -q -O scala-cli.gz  https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux-static.gz && gunzip scala-cli.gz
$ chmod +x scala-cli
$ mv scala-cli /usr/bin/
```

Check that it runs fine by running its `about` command:
```text
$ scala-cli about
```

## OS-specific packages

### Debian (x86-64)

#### Apt-based installation

```text
$ curl -s --compressed "https://virtuslabrnd.github.io/scala-cli-packages/KEY.gpg" | sudo apt-key add -
$ sudo curl -s --compressed -o /etc/apt/sources.list.d/scala_cli_packages.list "https://virtuslabrnd.github.io/scala-cli-packages/debian/scala_cli_packages.list"
$ sudo apt update
$ sudo apt install scala-cli
```

#### dpkg-based installation
The Debian package can be downloaded at [this address](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb).

Alternatively, you can download it and install it manually with:

```text
$ curl -fLo scala-cli.deb https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb
$ dpkg -i scala-cli.deb
```

### RPM (x86-64)

#### Yum-based installation

```text
$ cat > /etc/yum.repos.d/virtuslab.repo << EOF
[virtuslab-repo]
name=VirtusLab Repo
baseurl=https://virtuslabrnd.github.io/scala-cli-packages/fedora/Packages
enabled=1
gpgcheck=1
gpgkey=https://virtuslabrnd.github.io/scala-cli-packages/KEY.gpg
EOF
$ yum repo-pkgs virtuslab-repo list
$ yum install scala-cli    
```

#### Rpm-based installation

The RPM package can be downloaded at [this address](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.rpm).

Alternatively, you can download it and install it manually with:
```text
$ curl -fLo scala-cli.rpm https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.rpm
$ rpm -i scala-cli.rpm
```

### Windows (x86-64)

[Download installer](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.msi)

### macOS (pkg)

[Download package](https://github.com/VirtuslabRnD/scala-cli/releases/download/nightly/scala-cli-x86_64-apple-darwin.pkg)

Once downloaded, right-click on `scala-cli-x86_64-apple-darwin.pkg` from Finder, and choose "Open".

### macOS (brew)

Scala CLI can be installed via [homebrew](https://brew.sh) with

```text
$ brew install VirtuslabRnD/scala-cli/scala-cli 
```

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
