package scala.cli.internal

import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.logging.Logger
import org.scalajs.testing.adapter.{TestAdapterInitializer => TAI}

import java.nio.file.Path

import scala.build.internal.ScalaJsConfig
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.{global => ec}
import scala.concurrent.duration.Duration

final class ScalaJsLinker {

  def link(
    classPath: Array[Path],
    mainClassOrNull: String,
    addTestInitializer: Boolean,
    config: ScalaJsConfig,
    linkingDir: Path,
    logger: Logger
  ): Unit = {

    // adapted from https://github.com/scala-js/scala-js-cli/blob/729824848e25961a3d9a1cfe6ac0260745033148/src/main/scala/org/scalajs/cli/Scalajsld.scala#L158-L193

    val linker = StandardImpl.linker(config.config)

    val output = PathOutputDirectory(linkingDir)

    val cache = StandardImpl.irFileCache().newCache

    val mainInitializers = Option(mainClassOrNull).toSeq.map { mainClass =>
      ModuleInitializer.mainMethodWithArgs(mainClass, "main")
    }
    val testInitializers =
      if (addTestInitializer)
        Seq(ModuleInitializer.mainMethod(TAI.ModuleClassName, TAI.MainMethodName))
      else
        Nil

    val moduleInitializers = mainInitializers ++ testInitializers

    implicit val ec0 = ec
    val futureResult = PathIRContainer
      .fromClasspath(classPath.toVector)
      .flatMap(containers => cache.cached(containers._1))
      .flatMap(linker.link(_, moduleInitializers, output, logger))
    Await.result(futureResult, Duration.Inf)
  }

}
