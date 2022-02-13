package scala.build.errors

final class NetworkUnaccessibleScalaVersionError(
  val maybeScalaVersion: Option[String]
) extends ScalaVersionError(s"Most probably, right now the network is not accessible for processing the requested Scala version ${maybeScalaVersion.getOrElse("")}\n" +
      "Please try again later. Thank you.")
