package scala.cli.commands.util

import scala.build.options.{ScalacOpt, ShadowingSeq}
import scala.cli.commands.{ScalacExtraOptions, ScalacOptions}

object ScalacOptionsUtil {
  extension (opts: List[String]) {

    def withScalacExtraOptions(scalacExtra: ScalacExtraOptions): List[String] = {
      def maybeScalacExtraOption(
        get: ScalacExtraOptions => Boolean,
        scalacName: String
      ): Option[String] =
        if get(scalacExtra) && !opts.contains(scalacName) then Some(scalacName) else None
      val scalacHelp    = maybeScalacExtraOption(_.scalacHelp, "-help")
      val scalacVerbose = maybeScalacExtraOption(_.scalacVerbose, "-verbose")
      opts ++ scalacHelp ++ scalacVerbose
    }
    def toScalacOptShadowingSeq: ShadowingSeq[ScalacOpt] =
      ShadowingSeq.from(opts.filter(_.nonEmpty).map(ScalacOpt(_)))

    def getScalacPrefixOption(prefixKey: String): Option[String] =
      opts.find(_.startsWith(s"$prefixKey:")).map(_.stripPrefix(s"$prefixKey:"))

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
