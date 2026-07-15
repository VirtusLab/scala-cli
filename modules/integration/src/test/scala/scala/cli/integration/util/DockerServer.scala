package scala.cli.integration.util

// adapted from https://github.com/coursier/coursier/blob/2cb552ea934a100f52ce79f94278304c90d808a4/modules/proxy-tests/src/it/scala/coursier/test/DockerServer.scala

import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.{ExposedPort, HostConfig, Ports}
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient

import scala.util.Try

final case class DockerServer(
  base: String,
  shutdown: () => Unit,
  address: String
)

object DockerServer {
  def withServer[T](
    image: String,
    basePath: String,
    // can't find a way to get back a randomly assigned port (even following https://github.com/spotify/docker-client/issues/625)
    // so that one has to be specified
    portMapping: (Int, Int)
  )(f: DockerServer => T): T = {

    val (imagePort, hostPort) = portMapping
    val addr                  = s"localhost:$hostPort"

    def log(s: String): Unit =
      Console.err.println(s"[$image @ $addr] $s")

    val config     = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    val httpClient = new ZerodepDockerHttpClient.Builder()
      .dockerHost(config.getDockerHost)
      .sslConfig(config.getSSLConfig)
      .build()
    val docker = DockerClientImpl.getInstance(config, httpClient)

    docker.pullImageCmd(image).exec(new PullImageResultCallback()).awaitCompletion()

    val exposedPort = ExposedPort.tcp(imagePort)
    val ports       = new Ports()
    ports.bind(exposedPort, Ports.Binding.bindPort(hostPort))

    val hostConfig = HostConfig.newHostConfig().withPortBindings(ports)

    var idOpt = Option.empty[String]

    def shutdown(): Unit =
      for (id <- idOpt) {
        Try(docker.killContainerCmd(id).exec())
        docker.removeContainerCmd(id).exec()
        docker.close()
      }

    try {
      val id = docker.createContainerCmd(image)
        .withExposedPorts(exposedPort)
        .withHostConfig(hostConfig)
        .exec()
        .getId
      idOpt = Some(id)

      log(s"starting container $id")
      docker.startContainerCmd(id).exec()

      val base = s"http://localhost:$hostPort/$basePath"

      log(s"waiting for $image server to be up-and-running")

      Thread.sleep(2000L)

      val server = DockerServer(base, () => shutdown(), addr)
      f(server)
    }
    finally
      shutdown()
  }
}
