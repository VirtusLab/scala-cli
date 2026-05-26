package scala.build.internal

import scala.build.{Logger, Sources}

object ScriptUtils {
  private val ignoredPackageRoots = Set("scala", "java", "javax", "META-INF")

  final case class ScriptDescriptor(
    name: String,
    subPath: os.SubPath,
    filePath: os.Path,
    shadowedDependencyJars: Seq[String] = Nil
  )

  def findShadowedDependencyPackages(
    sources: Sources,
    classPath: Seq[os.Path],
    logger: Logger
  ): Seq[ScriptDescriptor] = {
    val scripts = sources.scriptTopLevelNames.filterNot(s => ignoredPackageRoots(s.name))
    if scripts.isEmpty then Nil
    else
      val packageRoots =
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
      scripts.flatMap { script =>
        packageRoots.get(script.name).map { jars =>
          script.copy(shadowedDependencyJars = jars.toSeq.map(_.last).sorted)
        }
      }
  }
}
