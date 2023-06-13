package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared._

// format: off
final case class PgpPushOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Recurse
    shared: SharedPgpPushPullOptions = SharedPgpPushPullOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions(),

  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Try to push the key even if Scala CLI thinks it's not a public key")
  @ExtraName("f")
    force: Boolean = false,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Whether to exit with code 0 if no key is passed")
    allowEmpty: Boolean = false,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("When running Scala CLI on the JVM, force running scala-cli-singing using a native launcher or a JVM launcher")
  @Hidden
    forceSigningExternally: Boolean = false
) extends HasGlobalOptions
// format: on

object PgpPushOptions {
  implicit lazy val parser: Parser[PgpPushOptions] = Parser.derive
  implicit lazy val help: Help[PgpPushOptions]     = Help.derive
}
