package scala.cli.commands.util

import scala.build.errors.{MainClassError, NoMainClassFoundError}
import scala.cli.commands.MainClassOptions

object MainClassOptionsUtil {
  implicit class MainClassOptionsOps(v: MainClassOptions) {
    def maybePrintMainClasses(mainClasses: Seq[String]): Either[MainClassError, Unit] =
      v.mainClassLs match {
        case Some(true) if mainClasses.nonEmpty =>
          println(mainClasses.mkString(" "))
          sys.exit(0)
        case Some(true) => Left(new NoMainClassFoundError)
        case _          => Right(())
      }
  }
}
