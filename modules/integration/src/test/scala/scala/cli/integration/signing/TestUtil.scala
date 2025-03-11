package scala.cli.signing.integration

object TestUtil {
  val cliPath: String  = sys.env.getOrElse("SIGNING_CLI", "cli")
  val cliKind: String  = sys.env.getOrElse("SIGNING_CLI_KIND", "jvm")
  val isCI: Boolean    = System.getenv("CI") != null
  val cli: Seq[String] = Seq(cliPath)
}
