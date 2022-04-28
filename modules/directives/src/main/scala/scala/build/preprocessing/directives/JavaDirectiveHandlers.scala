package scala.build.preprocessing.directives

import scala.build.{options => ops}
import scala.cli.commands.SharedJavaOptions
import scala.build.options.BuildOptions
import scala.build.options.ShadowingSeq
import scala.build.options.{BuildOptions, JavaOpt => M, JavaOptions, ShadowingSeq}
import scala.build.Positioned
import org.checkerframework.checker.units.qual.m
import scala.build.errors.BuildException
import scala.build.errors.WrongJavaHomePathError
import scala.util.*

object JavaDirectiveHandlers
    extends PrefixedDirectiveGroup[ops.JavaOptions]("java", "Java", SharedJavaOptions.help) {

  object Opt extends BaseStringListSetting:
    def processOption(strs: ::[Positioned[String]])(using Ctx) = 
        Right(JavaOptions(javaOpts = ShadowingSeq.from(strs.map(_.map(ops.JavaOpt(_))))))
    def exampleValues = Seq(Seq("-Xmx2g"), Seq("-Xms1g", "-Xnoclassgc"))
    def usageValue = "option"
    override def name = "Java Options"

  object Prop extends BaseStringListSetting:
    def processOption(strs: ::[Positioned[String]])(using Ctx) = 


        val props = strs.map(_.map { _.split("=") match 
                  case Array(k)    => ops.JavaOpt(s"-D$k")
                  case Array(k, v) => ops.JavaOpt(s"-D$k=$v")
              }
            )
        Right(JavaOptions(javaOpts = ShadowingSeq.from(strs.map(_.map(ops.JavaOpt(_))))))
    def exampleValues = Seq(Seq("-Xmx2g"), Seq("-Xms1g", "-Xnoclassgc"))
    def usageValue = "option"
    override def name = "Java Properties"


  object Home extends BaseStringSetting {
    def processOption(rawHome: Positioned[String])(using ctx: Ctx) = 
      for 
        root <- ctx.asRoot(rawHome)
        path <- rawHome.safeMap(p => os.Path(p, root), "Problem with processing java path")
      yield JavaOptions(javaHomeOpt = Some(path))
    
    def exampleValues = Seq("/home/user/jvm", "<path>")

    def usageValue = "path"
  }

  def group = DirectiveHandlerGroup("Java options", Seq(Opt, Prop, Home))

  def mkBuildOptions(parsed: JavaOptions) = BuildOptions(javaOptions = parsed)

}
