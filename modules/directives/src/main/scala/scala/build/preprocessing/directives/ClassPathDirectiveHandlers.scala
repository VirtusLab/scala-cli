package scala.build.preprocessing.directives

import scala.build.options.ClassPathOptions
import scala.cli.commands.SharedDependencyOptions
import scala.build.options.BuildOptions
import scala.build.Positioned
import dependency.AnyDependency
import dependency.parser.DependencyParser
import scala.build.errors.DependencyFormatError
import scala.build.errors.BuildException
import scala.build.options.ShadowingSeq
import coursier.parse.RepositoryParser

object ClassPathDirectiveHandlers extends PrefixedDirectiveGroup[ClassPathOptions](
      "",
      "Class PATH",
      SharedDependencyOptions.help
    ) {
  override def mkBuildOptions(c: ClassPathOptions) = BuildOptions(classPathOptions = c)
  override def group = DirectiveHandlerGroup("Class Path handling", Seq())

  object Jar extends BaseStringListSetting {
    def exampleValues = Seq(
      Seq(
        "/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar"
      ),
      Seq("libs/a.jar", "libs/b.jar")
    )
    def usageValue    = "jar_path"
    override def keys = Seq("jar", "jars")
    def processOption(paths: ::[Positioned[String]])(using ctx: Ctx) =
      for
        root  <- ctx.asRoot(paths.head)
        paths <- paths.map(_.safeMap(v => os.Path(v, root), "Incorrect path")).sequenceToComposite
      yield ClassPathOptions(extraClassPath = paths.map(_.value))
  }

  object Libs extends BaseStringListSetting {
    def exampleValues = Seq(
      Seq("org.scalatest::scalatest:3.2.10"),
      Seq("org.scalameta::munit:0.7.29", "org.scalameta::munit:0.7.29"),
      Seq(
        "tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar"
      )
    )

    def usageValue = "dependency"

    def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
      // Really necessary? (might already be handled by the coursier-dependency library)
      DependencyParser.parse(depStr.filter(!_.isSpaceChar))
        .left.map(err => new DependencyFormatError(depStr, err))

    override def keys = Seq("lib", "libs")

    def parseDependencies(paths: ::[Positioned[String]]) =
      paths.map(_.mapEither(parseDependency)).sequenceToComposite.map(ShadowingSeq.from)

    def processOption(paths: ::[Positioned[String]])(using ctx: Ctx) =
      parseDependencies(paths).map(deps => ClassPathOptions(extraDependencies = deps))
  }

  object Repository extends BaseStringListSetting {
    def exampleValues = Seq(
      Seq("jitpack"),
      Seq("sonatype:snapshots", "TODO"),
      Seq("https://maven-central.storage-download.googleapis.com/maven2")
    )

    def usageValue = "repository"

    override def keys = Seq("repository", "repositories")

    private def checkRepository(str: Positioned[String]): Either[BuildException, String] =
      RepositoryParser.repository(str.value).left.flatMap(str.error(_)).map(_ => str.value)

    def processOption(paths: ::[Positioned[String]])(using ctx: Ctx) =
      paths.map(checkRepository).sequenceToComposite.map(r =>
        ClassPathOptions(extraRepositories = r)
      )
  }

  object ResourceDir extends BaseStringListSetting {
    def exampleValues = Seq(
      Seq("resources"),
      Seq("/home/coder/globalResources", "localResources")
    )

    def usageValue = "directory"

    override def keys = Seq("resourceDir", "resourceDirs")

    private def checkRepository(str: Positioned[String]): Either[BuildException, String] =
      RepositoryParser.repository(str.value).left.flatMap(str.error(_)).map(_ => str.value)

    private def osRootResource(using ctx: Ctx): Either[os.SubPath, os.Path] =
      val cwd = ctx.scopedDirective.cwd
      cwd.root.left.map(_ => cwd.path).map(_ / cwd.path)

    def processOption(paths: ::[Positioned[String]])(using ctx: Ctx) =
      osRootResource match
        case Left(virtualRoot) =>
          val result = paths.map(p =>
            p.safeMap(path => virtualRoot / os.SubPath(path), "Invalid path")
          ).sequenceToComposite
          result.map(dirs => ClassPathOptions(resourcesVirtualDir = dirs.map(_.value)))
        case Right(root) =>
          val result =
            paths.map(_.safeMap(path => os.Path(path, root), "Invalid path")).sequenceToComposite
          for
            paths <- result
            path  <- paths if !os.exists(path.value)
          do
            ctx.logger.diagnostic(
              "Provided resource directory path doesn't exist",
              positions = path.positions
            )

          result.map(dirs => ClassPathOptions(resourcesDir = dirs.map(_.value)))
  }
}
