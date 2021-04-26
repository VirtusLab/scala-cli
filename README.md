
# scala-cli

[![Build status](https://github.com/VirtusLab/scala-cli/workflows/CI/badge.svg)](https://github.com/VirtusLab/scala-ci/actions?query=workflow%3ACI)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alexarchambault.scala-cli/cli_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.alexarchambault.scala-cli/cli_2.12)

## Developer docs

### Common commands

#### Running the CLI from sources

```text
$ ./mill -i 'cli[2.12.13].run' …arguments…
```

#### Run tests with the JVM launcher

```text
$ ./mill jvm-tests.test
```

Filter test suites with
```text
$ ./mill jvm-tests.test scala.cli.DefaultTests
$ ./mill jvm-tests.test 'scala.cli.DefaultTests.Multiple scripts*'
```

#### Run tests with the native launcher

(generating the launcher can take several minutes)

```text
$ ./mill native-tests.test
```

#### Generate Metals configuration files

```text
$ ./mill mill.contrib.Bloop/install
```

Then run the command "Metals: Connect to build server".

(Recommended over the Metals import project functionality.)

#### Generate a native launcher

```text
$ ./mill -i show 'cli[2.12.13].nativeImage'
```

### Website commands

#### Generate the website once

```text
$ cd website
$ yarn build
$ npm run serve
```

#### Generate the website continuously

```text
$ cd website
$ yarn run start
```
