package scala.build.options.packaging

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Positioned
import scala.build.errors.CompositeBuildException

class DockerOptionsTests extends munit.FunSuite {

  for port <- Seq("8080", "1", "65535", "9000-9010", "53/udp", "8080/tcp", "9000-9010/udp")
  do
    test(s"valid exposed port: $port") {
      expect(DockerOptions.validateExposedPorts(Seq(Positioned.none(port))) == Right(Seq(port)))
    }

  for port <-
      Seq("", "abc", "0", "65536", "9010-9000", "8080/sctp", "8080/TCP", "80-", "-80", " 80")
  do
    test(s"invalid exposed port: '$port'") {
      DockerOptions.validateExposedPorts(Seq(Positioned.none(port))) match
        case Left(e) =>
          expect(e.message.contains("docker-exposed-port"))
          expect(e.message.contains(s"\"$port\""))
        case Right(ports) => fail(s"Expected an error for '$port', got $ports")
    }

  test("all invalid exposed ports are reported") {
    val ports = Seq("8080", "abc", "0").map(Positioned.none)
    DockerOptions.validateExposedPorts(ports) match
      case Left(e: CompositeBuildException) =>
        val messages = e.exceptions.map(_.message)
        expect(messages.length == 2)
        expect(messages.exists(_.contains("\"abc\"")))
        expect(messages.exists(_.contains("\"0\"")))
      case other => fail(s"Expected a composite error, got $other")
  }
}
