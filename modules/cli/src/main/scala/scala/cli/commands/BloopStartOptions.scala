package scala.cli.commands

import caseapp._

import scala.build.blooprifle.BloopRifleConfig
import scala.build.options.BuildOptions
import scala.build.options.InternalOptions

// format: off
final case class BloopStartOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),

  @Name("f")
    force: Boolean = false
) {
  // format: on

  private def buildOptions: BuildOptions =
    BuildOptions(
      javaOptions = jvm.javaOptions,
      internal = InternalOptions(
        cache = Some(coursier.coursierCache(logging.logger.coursierLogger))
      )
    )

  def bloopRifleConfig(): BloopRifleConfig =
    compilationServer.bloopRifleConfig(
      logging.logger,
      logging.verbosity,
      buildOptions.javaCommand(),
      directories.directories
    )

}

object BloopStartOptions {
  implicit val parser = Parser[BloopStartOptions]
  implicit val help   = Help[BloopStartOptions]
}
