package scala.cli.commands.util

import scala.build.options.{ScalacOpt, ShadowingSeq}
import scala.cli.commands.ScalacOptions

object ScalacOptionsUtil {
  extension (opts: List[String]) {
    def toScalacOptShadowingSeq: ShadowingSeq[ScalacOpt] =
      ShadowingSeq.from(opts.filter(_.nonEmpty).map(ScalacOpt(_)))

  }

  extension (opts: ShadowingSeq[ScalacOpt]) {
    def filterScalacOptionKeys(f: String => Boolean): ShadowingSeq[ScalacOpt] =
      opts.filterKeys(_.key.exists(f))
    def filterNonRedirected: ShadowingSeq[ScalacOpt] =
      opts.filterScalacOptionKeys(!ScalacOptions.ScalaCliRedirectedOptions.contains(_))
    def getScalacOption(key: String): Option[String] =
      opts.get(ScalacOpt(key)).headOption.map(_.value)
  }
}
