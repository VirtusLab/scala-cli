package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, UnexpectedDirectiveError}
import scala.build.options.publish
import scala.build.options.{BuildOptions, PostBuildOptions, PublishOptions}
import scala.build.Positioned
import org.checkerframework.checker.units.qual.m
import javax.lang.model.util.Elements.Origin
import coursier.core.Info.Developer


object PublishDirectiveHandlers extends PrefixedDirectiveGroup[PublishOptions]("publish", "Publish", scala.cli.commands.PublishOptions.help){
  def mkBuildOptions(v: PublishOptions): BuildOptions = BuildOptions(notForBloopOptions = PostBuildOptions(publishOptions = v))

  class PublishSetting(
    parse: Positioned[String] => Either[BuildException, PublishOptions] | PublishOptions, 
    val exampleValues: Seq[String],
    val usageValue: String = "value"
    ) extends BaseStringSetting {
    

    def processOption(opts: Positioned[String])(using Ctx): Either[BuildException, PublishOptions] = parse(opts) match {
      case settings: PublishOptions => Right(settings)
      case e: Either[BuildException, PublishOptions] => e
    }
  }

  object Organization extends PublishSetting(
    v => PublishOptions(organization = Some(v)),
    Seq("com.githib.scala-cli"),
    "organization"
  )

  object Name extends PublishSetting(
    v => PublishOptions(name = Some(v)),
    Seq("scala-cli-core"),
    "name"
  )

  object Version extends PublishSetting(
    v => PublishOptions(version = Some(v)),
    Seq("1.0.1-RC2"),
    "version"
  )

  object Description extends PublishSetting(
    v => PublishOptions(description = Some(v.value)),
    Seq("Description of your repository"),
    "<description>"
  )

  object ComputeVersion extends PublishSetting(
    str => publish.ComputeVersion.parse(str).map(v => PublishOptions(computeVersion = Some(v))),
    Seq("git:tag", "git:tag:<repo>", "git:dynver", "git:dynver:<repo>", "command:<command>"),
    "git:(dynver|tag)[:repo] | comamnd:<command>3" // TODO custom 
  )

  object URL extends PublishSetting(
    v => v.safeMap(s => java.net.URI(s).toString, "Invalid URL").map(v => PublishOptions(url = Some(v))),
    Seq("https://scala-cli.virtuslab.org/"),
    "<url>"
  )

  object License extends PublishSetting(
    v => publish.License.parse(v).map(l => PublishOptions(license = Some(l))),
    Seq("Apache 2.0"),
    "<license>"
  )

  object VersionControl extends PublishSetting(
    v => publish.Vcs.parse(v).map(l => PublishOptions(versionControl = Some(l))),
    Seq("github:VirtusLab/scala-cli.git", "<url>|<connection>|<dev_connection>"),
    "github:<org>/<repo> | <url>|<connection>|<dev_connection>"
  ){
    override def keys = super.keys :+ "scm"
  }

  object ScalaVersionSuffix extends PublishSetting(
    v => PublishOptions(scalaVersionSuffix = Some(v.value)),
    Seq("_3"),
    "<suffix>"
  )

  object ScalaPlatformSuffix extends PublishSetting(
    v => PublishOptions(scalaPlatformSuffix = Some(v.value)),
    Seq("_js"),
    "<suffix>"
  )

  object Repository extends PublishSetting(
    v => PublishOptions(repository = Some(v.value)),
    Seq("https://repo.maven.apache.org/maven2"),
    "<repository>"
  )

  object GpgKey extends PublishSetting(
    v => PublishOptions(gpgSignatureId = Some(v.value)),
    Seq("user@email.com"),
    "<key>"
  )

  object Developers extends BaseStringListSetting{
    def processOption(opts: ::[Positioned[String]])(using Ctx): Either[BuildException, PublishOptions] = 
      opts.map(publish.Developer.parse).sequenceToComposite.map(parsed => PublishOptions(developers = parsed))
      
    def exampleValues = Seq(
      Seq("_example|Sarah Jones|https://docs.github.com/_example"),
      Seq("_example|Sarah Jones|https://docs.github.com/_example", "_example2|Nick Smith|https://docs.github.com/_example|nick@example.org")
    )

    def usageValue = "id|name|address[|email]"

    override def keys = Seq("developer", "developers")
  }


  object GpgOptions extends StringListSetting(
    opts => PublishOptions(gpgOptions =  opts.toList),
    Seq(Seq("--armor"), Seq("--local-user", "sarah_j")),
    "option"
  )

  def group = DirectiveHandlerGroup("Publish", Seq(Organization, Name, Version, ComputeVersion, URL, License, VersionControl, Developers, ScalaVersionSuffix, ScalaPlatformSuffix, Repository, GpgKey, GpgOptions))
}
