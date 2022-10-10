package sclicheck

object TestUtil {

  lazy val scalaCliPath = Option(System.getenv("SCLICHECK_SCALA_CLI")).getOrElse {
    sys.error("SCLICHECK_SCALA_CLI not set")
  }

}
