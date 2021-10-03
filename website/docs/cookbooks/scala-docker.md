---
title: Package Scala application as a docker image
sidebar_position: 5
---

Use `scala-cli` to package your application into a docker image. ScalaCLI automatically creates executable application and package them into docker image.

Here is a simple piece of code that will be executed in the docker container.

```scala title:HelloWorld.scala
object HelloWorld extends App {
    println("Hello from Docker")
}
```


Passing `--docker` to the `package` sub-command generates a docker image. The docker image name parameter `--docker-image-repository` is mandatory.

The following command will generate `hello-docker` image with `latest` tag:
```bash ignore
scala-cli package --docker HelloWorld.scala --docker-image-repository hello-docker
```

```bash ignore
docker run hello-docker
# Hello from Docker
```

It is also supported to package your app in `JS` or `Native` environments.

```bash ignore
scala-cli package --js --docker HelloWorld.scala --docker-image-repository hello-docker
```

```bash ignore
scala-cli package --native --docker HelloWorld.scala --docker-image-repository hello-docker
```