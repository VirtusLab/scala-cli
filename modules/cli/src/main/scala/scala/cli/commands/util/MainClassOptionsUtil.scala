package scala.cli.commands.util

import scala.build.errors.{MainClassError, NoMainClassFoundError}
import scala.cli.commands.MainClassOptions

object MainClassOptionsUtil {
  implicit class MainClassOptionsOps(v: MainClassOptions) {
    def maybePrintMainClasses(
      mainClasses: Seq[String],
      shouldExit: Boolean = true
    ): Either[MainClassError, Unit] =
      v.mainClassLs match {
        case Some(true) if mainClasses.nonEmpty =>
          println(mainClasses.mkString(" "))
          if (shouldExit) sys.exit(0)
          else Right(())
        case Some(true) => Left(new NoMainClassFoundError)
        case _          => Right(())
      }
  }
}
