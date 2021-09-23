---
title: Installation
sidebar_position: 2
---

We recomment following the [Quick start](#quick-start) instructions
to install Scala CLI. If the quick start instructions don't suit you,
more installation options are offered in the [Advanced](#advanced) section.

import DownloadButton from '../src/components/DownloadButton';

## Quick start

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs 
    groupId="operating-systems"
    defaultValue="win"
   values={[
    {label: 'Windows', value: 'win'},
    {label: 'macOS/Linux', value: 'mac/linux'},
  ]}
>
<TabItem value="win">

Download Scala CLI for Windows
<DownloadButton desc= 'Scala CLI for Windows' href='https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.msi'></DownloadButton>
</TabItem>
<TabItem value="mac/linux">

Run the following one-line command in your terminal

```bash
curl -sSf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh
```
</TabItem>
</Tabs>

## Advanced

<Tabs
groupId="operating-systems-specific"
defaultValue="windows"
values={[
{label: 'Windows', value: 'windows'},
{label: 'MacOs', value: 'macOs'},
{label: 'Linux', value: 'linux'},
]}
>
<TabItem value="linux">

<Tabs
groupId="linux"
defaultValue="manual"
values={[
{label: 'Manual', value: 'manual'},
{label: 'Snap', value: 'snap'},
{label: 'Apt', value: 'apt'},
{label: 'Deb', value: 'deb'},
{label: 'Yum', value: 'yum'},
{label: 'Rpm', value: 'rpm'},
{label: 'Alpine', value: 'alpine'},
]}
>
<TabItem value="manual">

Download the launcher from GitHub release assets with
```bash
curl -fL https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.gz | gzip -d > scala-cli
chmod +x scala-cli
sudo mv scala-cli /usr/local/bin/scala-cli
```

Check that it runs fine by running its `about` command:
```bash
scala-cli about
```
</TabItem>

<TabItem value="apt">

Scala CLI can be installed via [apt](https://wiki.debian.org/Apt) packager tool.

```bash
curl -s --compressed "https://virtuslab.github.io/scala-cli-packages/KEY.gpg" | sudo apt-key add -
sudo curl -s --compressed -o /etc/apt/sources.list.d/scala_cli_packages.list "https://virtuslab.github.io/scala-cli-packages/debian/scala_cli_packages.list"
sudo apt update
sudo apt install scala-cli
```
</TabItem>
<TabItem value="snap">

Scala CLI can be installed via [snap](https://snapcraft.io/#) with

```bash
snap install scala-cli
```
</TabItem>
<TabItem value="deb">

The Debian package can be downloaded at [this address](https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb).

Alternatively, you can download it and install it manually with:

```bash
curl -fLo scala-cli.deb https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb
sudo dpkg -i scala-cli.deb
```
</TabItem>

<TabItem value="yum">

Scala CLI can be installed via [yum](http://yum.baseurl.org) packager tool.

```bash
sudo cat > /etc/yum.repos.d/virtuslab.repo << EOF
[virtuslab-repo]
name=VirtusLab Repo
baseurl=https://virtuslab.github.io/scala-cli-packages/CentOS/Packages
enabled=1
gpgcheck=1
gpgkey=https://virtuslab.github.io/scala-cli-packages/KEY.gpg
EOF
sudo yum repo-pkgs virtuslab-repo list
sudo yum install scala-cli    
```
</TabItem>
<TabItem value="rpm">

The RPM package can be downloaded at [this address](https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.rpm).

Alternatively, you can download it and install it manually with:
```bash
curl -fLo scala-cli.rpm https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.rpm
sudo rpm -i scala-cli.rpm
```
</TabItem>
<TabItem value="alpine">

Download the launcher from GitHub release assets with

```bash
wget -q -O scala-cli.gz  https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux-static.gz && gunzip scala-cli.gz
chmod +x scala-cli
mv scala-cli /usr/bin/
```

Check that it runs fine by running its `about` command:
```bash
scala-cli about
```
</TabItem>
</Tabs>

</TabItem>
<TabItem value="windows">

<Tabs
groupId="windows"
defaultValue="manual"
values={[
{label: 'Manual', value: 'manual'},
{label: 'Installer', value: 'installer'},
]}
>

<TabItem value="manual">

Note that the Windows manual installation requires [Visual C++ redistributable](https://support.microsoft.com/en-us/topic/the-latest-supported-visual-c-downloads-2647da03-1eea-4433-9aff-95f26a218cc0)
to be installed. See below for how to install it.

Download the launcher from GitHub release assets with
```bash
curl -fLo scala-cli.zip https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.zip
tar -xf scala-cli.zip
```

Check that it runs fine by running its `about` command:
```bash
scala-cli about
```

If you get an error about `MSVCR100.dll` being missing, you have to install
[Visual C++ redistributable](https://support.microsoft.com/en-us/topic/the-latest-supported-visual-c-downloads-2647da03-1eea-4433-9aff-95f26a218cc0). A valid version is distributed with the Scala CLI launchers.
You can download it [here](https://github.com/Virtuslab/scala-cli/releases/download/nightly/vc_redist.x64.exe),
and install it by double-clicking on it. Once the Visual C++ redistributable runtime is installed,
check that the Scala CLI runs fine by running its `about` command:
```bash
scala-cli about
```

Note that the commands above don't put the `scala-cli` command in the `PATH`. For that, you can create a directory, move the
launcher there, and add the directory to the `PATH` with
```bash
md "%USERPROFILE%/scala-cli"
scala-cli add-path "%USERPROFILE%/scala-cli"
move scala-cli.exe "%USERPROFILE%/scala-cli"
```
</TabItem>
<TabItem value="installer">

Download MSI installer with Scala CLI for Windows
<DownloadButton desc= 'Scala CLI for Windows' href='https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.msi'></DownloadButton>
</TabItem>
</Tabs>

</TabItem>
<TabItem value="macOs">

<Tabs
groupId="macOs"
defaultValue="manual"
values={[
{label: 'Manual', value: 'manual'},
{label: 'Installer', value: 'installer'},
{label: 'Brew', value: 'brew'},
]}
>
<TabItem value="manual">

Download the launcher from GitHub release assets with
```bash
curl -fL https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-apple-darwin.gz | gzip -d > scala-cli
chmod +x scala-cli
mv scala-cli /usr/local/bin/scala-cli
```

Check that it runs fine by running its `about` command:
```bash
scala-cli about
```
</TabItem>
<TabItem value="installer">

Download PKG installer with Scala CLI for MacOS
<DownloadButton desc= 'Scala CLI for MacOS' href='https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-apple-darwin.pkg'></DownloadButton>
<br/>
<br/>

Once downloaded, right-click on `scala-cli-x86_64-apple-darwin.pkg` from Finder, and choose "Open".
</TabItem>
<TabItem value="brew">

Scala CLI can be installed via [homebrew](https://brew.sh) with

```bash
brew install Virtuslab/scala-cli/scala-cli 
```
</TabItem>
</Tabs>

</TabItem>
</Tabs>

## Shell completions

Only bash and zsh completions are offered for now.

Try the completions with
```
eval "$(scala-cli install completions --env)"
scala-cli --<TAB>
```

Install them on your system with
```bash
scala-cli install completions
```

If any of the `scala-cli install completions` command complained that your shell cannot be determined, specify it
with `--shell`, like
```bash
scala-cli install completions --shell zsh
```
