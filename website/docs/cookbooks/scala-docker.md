---
title: Packaging Scala applications as Docker images
sidebar_position: 5
---

Scala CLI can create an executable application and package it into a Docker image.

For example, here's a simple piece of code that will be executed in a Docker container:

```scala title=HelloDocker.scala
object HelloDocker extends App {
    println("Hello from Docker")
}
```

Passing `--docker` to the `package` sub-command generates a Docker image. When creating a Docker image, the `--docker-image-repository` parameter is mandatory.

The following command generates a `hello-docker` image with the `latest` tag:

```bash
scala-cli package --docker HelloDocker.scala --docker-image-repository hello-docker
```

<!-- Expected:
Started building docker image with your application, it would take some time
Built docker image, run it with
  docker run hello-docker:latest
-->

```bash
docker run hello-docker
# Hello from Docker
```

<!-- Expected:
Hello from Docker
-->

You can also package your app in the Scala.JS or Scala Native environments.
For example, this command creates a Scala.JS Docker image:

```bash
scala-cli package --js --docker HelloDocker.scala --docker-image-repository hello-docker
```
<!-- Expected:
Started building docker image with your application, it would take some time
Built docker image, run it with
  docker run hello-docker:latest
-->

This command creates a Scala Native Docker image:

```bash ignore
scala-cli package --native --docker HelloDocker.scala --docker-image-repository hello-docker
```

:::note
Packaging a Scala Native application to a Docker image is supported only on Linux.
:::
