// originally adapted from https://github.com/joan38/mill-scalafix/blob/9b6f577e6ab546cb811e327a101adc9fb893a1ca/mill-scalafix/src/com/goyeau/mill/scalafix/ScalafixModule.scala

import com.goyeau.mill.scalafix.{BuildInfo, CoursierUtils}
import coursier.Repository
import mill.{Agg, T}
import mill.api.{Logger, Loose, PathRef, Result}
import mill.scalalib.{Dep, DepSyntax, ScalaModule}
import mill.define.{Command, Target}
import mill.scalalib.api.Util.isScala3
import os._
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixError._
import scala.compat.java8.OptionConverters._
import scala.jdk.CollectionConverters._

trait ScalafixModule extends ScalaModule {
  override def scalacPluginIvyDeps: Target[Loose.Agg[Dep]] = super.scalacPluginIvyDeps() ++
    (if (isScala3(scalaVersion())) Agg.empty
     else Agg(ivy"org.scalameta:::semanticdb-scalac:${BuildInfo.semanticdbScalac}"))

  override def scalacOptions: Target[Seq[String]] = super.scalacOptions() ++
    (if (isScala3(scalaVersion())) Seq("-Xsemanticdb") else Seq.empty)

  def scalafixConfig: T[Option[Path]]       = T(None)
  def scalafixIvyDeps: T[Agg[Dep]]          = Agg.empty[Dep]
  def scalafixScalaBinaryVersion: T[String] = "2.12"

  def scalafixSourceRoot: T[PathRef] = T {
    val sources0 = allSources()
      .map(_.path)
      .filter(os.exists(_))
    val sourceRoot = sources0.find(_.last == "scala")
      .orElse(sources0.headOption)
      .getOrElse(millSourcePath)
    PathRef(sourceRoot)
  }

  def fix(args: String*): Command[Unit] =
    T.command {
      ScalafixModule.fixAction(
        T.ctx().log,
        repositoriesTask(),
        ScalafixModule.filesToFix(sources()).map(_.path),
        localClasspath().map(_.path),
        scalaVersion(),
        scalafixScalaBinaryVersion(),
        scalacOptions(),
        scalafixIvyDeps(),
        scalafixConfig(),
        scalafixSourceRoot().path,
        args: _*
      )
    }
}

object ScalafixModule {
  def fixAction(
    log: Logger,
    repositories: Seq[Repository],
    sources: Seq[Path],
    classpath: Seq[Path],
    scalaVersion: String,
    scalaBinaryVersion: String,
    scalacOptions: Seq[String],
    scalafixIvyDeps: Agg[Dep],
    scalafixConfig: Option[Path],
    sourceRoot: os.Path,
    args: String*
  ): Result[Unit] = {
    val (inSourceRoot, outsideSourceRoot) = sources.partition(_.startsWith(sourceRoot))
    if (outsideSourceRoot.nonEmpty) {
      log.error(
        s"Ignoring ${outsideSourceRoot.length} source file(s) outside of source root $sourceRoot:"
      )
      for (s <- outsideSourceRoot)
        log.error(s"  $s")
    }
    if (inSourceRoot.nonEmpty) {
      val scalafix = Scalafix
        .fetchAndClassloadInstance(
          scalaBinaryVersion,
          repositories.map(CoursierUtils.toApiRepository).asJava
        )
        .newArguments()
        .withParsedArguments(args.asJava)
        .withWorkingDirectory(os.pwd.toNIO)
        .withConfig(scalafixConfig.map(_.toNIO).asJava)
        .withClasspath(classpath.map(_.toNIO).asJava)
        .withScalaVersion(scalaVersion)
        .withScalacOptions(scalacOptions.asJava)
        .withPaths(inSourceRoot.map(_.toNIO).asJava)
        .withSourceroot(sourceRoot.toNIO)
        .withToolClasspath(
          Seq.empty.asJava,
          scalafixIvyDeps.map(CoursierUtils.toCoordinates).iterator.toSeq.asJava,
          repositories.map(CoursierUtils.toApiRepository).asJava
        )

      log.info(
        s"Rewriting and linting ${sources.size} Scala sources against ${scalafix.rulesThatWillRun.size} rules"
      )
      val errors = scalafix.run()
      if (errors.isEmpty) Result.Success(())
      else {
        val errorMessages = errors.map {
          case ParseError => "A source file failed to be parsed"
          case CommandLineError =>
            scalafix.validate().asScala.fold("A command-line argument was parsed incorrectly")(
              _.getMessage
            )
          case MissingSemanticdbError =>
            "A semantic rewrite was run on a source file that has no associated META-INF/semanticdb/.../*.semanticdb"
          case StaleSemanticdbError =>
            """The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
              |To resolve this error re-compile the project and re-run Scalafix""".stripMargin
          case TestError =>
            "A Scalafix test error was reported. Run `fix` without `--check` or `--diff` to fix the error"
          case LinterError  => "A Scalafix linter error was reported"
          case NoFilesError => "No files were provided to Scalafix so nothing happened"
          case _            => "Something unexpected happened running Scalafix"
        }
        Result.Failure(errorMessages.mkString("\n"))
      }
    }
    else Result.Success(())
  }

  def filesToFix(sources: Seq[PathRef]): Seq[PathRef] =
    for {
      pathRef <- sources if os.exists(pathRef.path)
      file <-
        if (os.isDir(pathRef.path))
          os.walk(pathRef.path).filter(file => os.isFile(file) && (file.ext == "scala"))
        else Seq(pathRef.path)
    } yield PathRef(file)
}
