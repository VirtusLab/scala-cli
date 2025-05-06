package scala.cli.commands.util
import scala.build.Logger
import scala.build.options.ScalacOpt.{filterScalacOptionKeys, noDashPrefixes}
import scala.build.options.{ScalacOpt, ShadowingSeq}
import scala.cli.commands.bloop.BloopExit
import scala.cli.commands.default.LegacyScalaOptions
import scala.cli.commands.shared.ScalacOptions.YScriptRunnerOption
import scala.cli.commands.shared.{ScalacExtraOptions, ScalacOptions}

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

    def getScalacOption(key: String): Option[String] = opts.toScalacOptShadowingSeq.getOption(key)

  }

  extension (opts: ShadowingSeq[ScalacOpt]) {
    def filterNonRedirected: ShadowingSeq[ScalacOpt] =
      opts.filterScalacOptionKeys(k =>
        !ScalacOptions.ScalaCliRedirectedOptions.contains(k.noDashPrefixes)
      )
    def filterNonDeprecated: ShadowingSeq[ScalacOpt] =
      opts.filterScalacOptionKeys(k =>
        !ScalacOptions.ScalacDeprecatedOptions.contains(k.noDashPrefixes)
      )
    def getOption(key: String): Option[String] =
      opts.get(ScalacOpt(key)).headOption.map(_.value)
  }
}
