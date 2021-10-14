<!--
  File was generated from based on docs/cookbooks/scala-docker.md, do not edit manually!
-->


# Package Scala application as a docker image

ScalaCLI can create an executable application and package it into a docker image.

Here is a simple piece of code that will be executed in the docker container.

```scala title=HelloDocker.scala
object HelloDocker extends App {
    println("Hello from Docker")
}
```

Passing `--docker` to the `package` sub-command generates a docker image. The docker image name parameter `--docker-image-repository` is mandatory.

The following command will generate `hello-docker` image with `latest` tag:

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

It is also supported to package your app in `JS` or `Native` environments.

```bash
scala-cli package --js --docker HelloDocker.scala --docker-image-repository hello-docker
```
<!-- Expected:
Started building docker image with your application, it would take some time
Built docker image, run it with
  docker run hello-docker:latest
-->

Package scala native application to docker image is supported only on Linux.

```bash ignore
scala-cli package --native --docker HelloDocker.scala --docker-image-repository hello-docker
```