package scala.build.internal

import scala.build.{Logger, Sources}

object ScriptUtils {
  private val ignoredPackageRoots = Set("scala", "java", "javax", "META-INF")

  final case class ScriptDescriptor(
    name: String,
    subPath: os.SubPath,
    filePath: os.Path,
    shadowedDependencyJars: Seq[String] = Nil,
    clashingLocalSources: Seq[os.SubPath] = Nil
  )

  def findShadowingClashes(
    sources: Sources,
    classPath: Seq[os.Path],
    logger: Logger
  ): Seq[ScriptDescriptor] = {
    val scripts = sources.scriptTopLevelNames.filterNot(s => ignoredPackageRoots(s.name))
    if scripts.isEmpty then Nil
    else
      val localCandidates = localTopLevelCandidates(sources)
      val packageRoots    = topLevelPackageRoots(classPath, logger)
      scripts.flatMap { script =>
        val deps   = packageRoots.getOrElse(script.name, Set.empty).toSeq.map(_.last).sorted
        val locals = localCandidates.getOrElse(script.name, Nil).filterNot(_ == script.subPath)
        Option.when(deps.nonEmpty || locals.nonEmpty) {
          script.copy(shadowedDependencyJars = deps, clashingLocalSources = locals)
        }
      }
  }

  private def localTopLevelCandidates(sources: Sources): Map[String, Seq[os.SubPath]] =
    (sources.paths.map((_, rel) => (baseName(rel.last), os.SubPath(rel.segments.toIndexedSeq))) ++
      sources.inMemory.collect {
        case Sources.InMemory(Right((subPath, _)), _, _, Some(_)) =>
          (baseName(subPath.last), subPath)
      })
      .groupMap(_._1)(_._2)

  private def baseName(fileName: String): String =
    val dot = fileName.lastIndexOf('.')
    if dot > 0 then fileName.take(dot) else fileName

  private def topLevelPackageRoots(
    classPath: Seq[os.Path],
    logger: Logger
  ): Map[String, Set[os.Path]] =
    classPath
      .filter(_.last.endsWith(".jar"))
      .flatMap(path =>
        JarUtils.walkClassEntries(path, logger) { (name, _) =>
          val slashIdx = name.indexOf('/')
          if slashIdx > 0 then Iterator.single(name.take(slashIdx))
          else Iterator.empty
        }.toSet.map(_ -> path)
      )
      .groupMap((root, _) => root)((_, path) => path)
      .view.mapValues(_.toSet).toMap

}
