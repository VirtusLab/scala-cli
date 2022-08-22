package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingResourcesDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Resource directories"
  def description = "Manually add a resource directory to the class path"
  def usage = """//> using resource _path_
                |
                |//> using resources _path1_, _path2_ …""".stripMargin
  override def usageMd =
    """`//> using resourceDir `_path_
      |
      |`//> using resourceDirs `_path1_, _path2_ …""".stripMargin
  override def examples = Seq(
    "//> using resourceDir \"./resources\""
  )

  override def isRestricted = false

  def keys = Seq("resourceDir", "resourceDirs")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val paths = groupedValues.scopedStringValues

      val (virtualRootOpt, rootOpt) = Directive.osRootResource(scopedDirective.cwd)

      val paths0 = rootOpt
        .toList
        .flatMap(root =>
          paths.map(_.positioned.value)
            .map(os.Path(_, root))
        )
      val virtualPaths = virtualRootOpt.map(virtualRoot =>
        paths.map(_.positioned.value).map(path => virtualRoot / os.SubPath(path))
      )
      warnIfNotExistsPath(paths0, logger)

      ProcessedDirective(
        Some(BuildOptions(
          classPathOptions = ClassPathOptions(
            resourcesDir = paths0,
            resourcesVirtualDir = virtualPaths.toList.flatten
          )
        )),
        Seq.empty
      )
    }

  private def warnIfNotExistsPath(paths: Seq[os.Path], logger: Logger): Unit = {
    paths.foreach(path =>
      if (!os.exists(path))
        logger.message(s"WARNING: provided resource directory path doesn't exist: $path")
    )
  }

}
