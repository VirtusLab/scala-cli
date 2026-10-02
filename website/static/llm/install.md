# Install Scala CLI

Official install page: https://scala-cli.virtuslab.org/install

## macOS (Homebrew)

```bash
brew install Virtuslab/scala-cli/scala-cli
```

## Linux

```bash
curl -sSLf https://scala-cli.virtuslab.org/get | sh
```

## Windows (WinGet)

```bash
winget install virtuslab.scalacli
```

## GitHub Actions

```yaml
steps:
  - uses: coursier/cache-action@v6
  - uses: VirtusLab/scala-cli-setup@main
```

## Verify

```bash
scala-cli version
# or, when using the default Scala runner:
scala version
```

## Advanced installation

Prefer another method (SDKMAN, coursier, native binaries, Docker, …)? See:

https://scala-cli.virtuslab.org/install#advanced-installation
