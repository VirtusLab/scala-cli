package scala.cli.internal

import org.scalajs.testing.adapter.{TestAdapterInitializer => TAI}

import scala.build.Logger
import scala.build.errors.ScalaJsLinkingError
import scala.build.internal.{Runner, ScalaJsLinkerConfig}

object ScalaJsLinker {

  def link(
    javaCommand: String,
    javaArgs: Seq[String],
    linkerClassPath: Seq[os.Path],
    classPath: Seq[os.Path],
    mainClassOrNull: String,
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    linkingDir: os.Path,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger
  ): Either[ScalaJsLinkingError, Unit] = {

    val outputArgs = Seq("--outputDir", linkingDir.toString)
    val mainClassArgs =
      Option(mainClassOrNull).toSeq.flatMap(mainClass => Seq("--mainMethod", mainClass + ".main"))
    val testInitializerArgs =
      if (addTestInitializer)
        Seq("--mainMethod", TAI.ModuleClassName + "." + TAI.MainMethodName + "!")
      else
        Nil
    // FIXME Fatal asInstanceOfs should be the default, but it seems we can't
    // pass Unchecked via the CLI here
    // It seems we can't pass the other semantics fields either.
    val semanticsArgs =
      if (config.semantics.asInstanceOfs == ScalaJsLinkerConfig.CheckedBehavior.Compliant)
        Seq("--compliantAsInstanceOfs")
      else
        Nil
    val moduleKindArgs       = Seq("--moduleKind", config.moduleKind)
    val moduleSplitStyleArgs = Seq("--moduleSplitStyle", config.moduleSplitStyle)
    val esFeaturesArgs =
      if (config.esFeatures.esVersion == ScalaJsLinkerConfig.ESVersion.ES2015)
        Seq("--es2015")
      else
        Nil
    val checkIRArgs = if (config.checkIR) Seq("--checkIR") else Nil
    val optArg =
      if (noOpt) "--noOpt"
      else if (fullOpt) "--fullOpt"
      else "--fastOpt"
    val sourceMapArgs = if (config.sourceMap) Seq("--sourceMap") else Nil
    val relativizeSourceMapBaseArgs =
      config.relativizeSourceMapBase.toSeq
        .flatMap(uri => Seq("--relativizeSourceMap", uri))
    val prettyPrintArgs =
      if (config.prettyPrint) Seq("--prettyPrint")
      else Nil
    val configArgs = Seq[os.Shellable](
      semanticsArgs,
      moduleKindArgs,
      moduleSplitStyleArgs,
      esFeaturesArgs,
      checkIRArgs,
      optArg,
      sourceMapArgs,
      relativizeSourceMapBaseArgs,
      prettyPrintArgs
    )

    val allArgs = Seq[os.Shellable](
      outputArgs,
      mainClassArgs,
      testInitializerArgs,
      configArgs,
      classPath.map(_.toString)
    )

    // FIXME In quiet mode, silence the output of that?
    val retCode = Runner.runJvm(
      javaCommand,
      javaArgs,
      linkerClassPath.map(_.toIO),
      "org.scalajs.cli.Scalajsld",
      allArgs.flatMap(_.value),
      logger
    )

    if (retCode == 0) {
      logger.debug("Scala.JS linker ran successfully")
      Right(())
    }
    else {
      logger.debug(s"Scala.JS linker exited with return code $retCode")
      Left(new ScalaJsLinkingError)
    }
  }

}
