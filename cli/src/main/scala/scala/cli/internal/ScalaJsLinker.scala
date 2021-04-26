package scala.cli.internal

import org.scalajs.linker.interface.{ESFeatures, LinkerOutput, ModuleInitializer, ModuleKind, Semantics, StandardConfig}
import org.scalajs.linker.{PathIRContainer, PathOutputFile, StandardImpl}
import org.scalajs.logging.{Level, ScalaConsoleLogger}

import java.nio.file.Path
import java.net.URI

final class ScalaJsLinker {

  def link(
    classPath: Array[Path],
    mainClass: String,
    dest: Path
  ): Unit = {

    // adapted from https://github.com/scala-js/scala-js-cli/blob/729824848e25961a3d9a1cfe6ac0260745033148/src/main/scala/org/scalajs/cli/Scalajsld.scala#L158-L193

    val config = StandardConfig()
      .withSemantics(Semantics.Defaults)
      .withModuleKind(ModuleKind.NoModule)
      .withESFeatures(ESFeatures.Defaults)
      .withCheckIR(false)
      .withOptimizer(true)
      .withParallel(true)
      .withSourceMap(false)
      .withRelativizeSourceMapBase(None)
      .withClosureCompiler(false)
      .withPrettyPrint(false)
      .withBatchMode(true)

    val linker = StandardImpl.linker(config)

    def relURI(f: Path) =
      new URI(null, null, f.getFileName.toString, null)

    val sm = dest.resolveSibling(dest.getFileName.toString + ".map")
    val output = LinkerOutput(PathOutputFile(dest))
      .withSourceMap(PathOutputFile(sm))
      .withSourceMapURI(relURI(sm))
      .withJSFileURI(relURI(dest))

    val cache = StandardImpl.irFileCache().newCache

    val moduleInitializers = Seq(
      ModuleInitializer.mainMethodWithArgs(mainClass, "main")
    )

    val logger = new ScalaConsoleLogger(Level.Info)

    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    import scala.concurrent.ExecutionContext.Implicits.global
    val futureResult = PathIRContainer
      .fromClasspath(classPath)
      .flatMap(containers => cache.cached(containers._1))
      .flatMap(linker.link(_, moduleInitializers, output, logger))
    Await.result(futureResult, Duration.Inf)
  }

}