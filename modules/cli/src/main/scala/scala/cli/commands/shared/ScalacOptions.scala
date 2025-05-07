package scala.cli.commands.shared

import caseapp.*
import caseapp.core.Scala3Helpers.*
import caseapp.core.parser.{Argument, ConsParser, NilParser, StandardArgument}
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.build.options.ScalacOpt.noDashPrefixes
import scala.cli.commands.tags

// format: off
final case class ScalacOptions(
  @Recurse
  argsFiles: List[ArgFileOption] = Nil,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Add a scalac option")
  @ValueDescription("option")
  @Name("O")
  @Name("scala-opt")
  @Name("scala-option")
  @Tag(tags.must)
    scalacOption: List[String] = Nil,
)
// format: on

object ScalacOptions {
  extension (opt: String) {
    private def hasValidScalacOptionDashes: Boolean =
      opt.startsWith("-") && opt.length > 1 && (
        if opt.length > 2 then opt.charAt(2) != '-'
        else opt.charAt(1) != '-'
      )
  }

  private val scalacOptionsArg = Arg("scalacOption").copy(
    extraNames = Seq(Name("scala-opt"), Name("O"), Name("scala-option")),
    valueDescription = Some(ValueDescription("option")),
    helpMessage = Some(HelpMessage(
      "Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`."
    )),
    group = Some(Group("Scala")),
    origin = Some("ScalacOptions")
  )
  // .withIsFlag(true) // The scalac options we handle accept no value after the -… argument
  val YScriptRunnerOption               = "Yscriptrunner"
  private val scalacOptionsPurePrefixes = Set("V", "W", "X", "Y")
  private val scalacOptionsPrefixes     = Set("P") ++ scalacOptionsPurePrefixes
  val replInitScript                    = "repl-init-script"
  private val replAliasedOptions        = Set(replInitScript)
  private val scalacAliasedOptions = // these options don't require being passed after -O and accept an arg
    Set(
      "bootclasspath",
      "boot-class-path",
      "coverage-exclude-classlikes",
      "coverage-exclude-files",
      "encoding",
      "extdirs",
      "extension-directories",
      "javabootclasspath",
      "java-boot-class-path",
      "javaextdirs",
      "java-extension-directories",
      "java-output-version",
      "release",
      "color",
      "g",
      "language",
      "opt",
      "pagewidth",
      "page-width",
      "target",
      "scalajs-mapSourceURI",
      "scalajs-genStaticForwardersForNonTopLevelObjects",
      "source",
      "sourcepath",
      "source-path",
      "sourceroot",
      YScriptRunnerOption
    ) ++ replAliasedOptions
  private val scalacNoArgAliasedOptions = // these options don't require being passed after -O and don't accept an arg
    Set(
      "experimental",
      "explain",
      "explaintypes",
      "explain-types",
      "explain-cyclic",
      "from-tasty",
      "unchecked",
      "nowarn",
      "no-warnings",
      "feature",
      "deprecation",
      "rewrite",
      "scalajs",
      "old-syntax",
      "print-tasty",
      "print-lines",
      "new-syntax",
      "indent",
      "no-indent",
      "preview",
      "uniqid",
      "unique-id"
    )

  /** This includes all the scalac options which disregard inputs and print a help and/or context
    * message instead.
    */
  val ScalacPrintOptions: Set[String] =
    scalacOptionsPurePrefixes ++ Set(
      "help",
      "opt:help",
      "Xshow-phases",
      "Xsource:help",
      "Xplugin-list",
      "Xmixin-force-forwarders:help",
      "Xlint:help",
      "Vphases"
    )

  /** This includes all the scalac options which are redirected to native Scala CLI options. */
  val ScalaCliRedirectedOptions: Set[String] = Set(
    "classpath",
    "cp",         // redirected to --extra-jars
    "class-path", // redirected to --extra-jars
    "d"           // redirected to --compilation-output
  )
  val ScalacDeprecatedOptions: Set[String] = Set(
    YScriptRunnerOption // old 'scala' runner specific, no longer supported
  )

  private val scalacOptionsArgument: Argument[List[String]] =
    new Argument[List[String]] {

      val underlying: StandardArgument[List[String]] = StandardArgument(scalacOptionsArg)

      val arg: Arg = scalacOptionsArg

      def withDefaultOrigin(origin: String): Argument[List[String]] = this
      def init: Option[List[String]]                                = Some(Nil)
      def step(
        args: List[String],
        index: Int,
        acc: Option[List[String]],
        formatter: Formatter[Name]
      ): Either[(Error, List[String]), Option[(Option[List[String]], List[String])]] =
        args match {
          case h :: t
              if h.hasValidScalacOptionDashes &&
              scalacOptionsPrefixes.exists(h.noDashPrefixes.startsWith) &&
              !ScalacDeprecatedOptions.contains(h.noDashPrefixes) =>
            Right(Some((Some(acc.getOrElse(Nil) :+ h), t)))
          case h :: t
              if h.hasValidScalacOptionDashes &&
              scalacNoArgAliasedOptions.contains(h.noDashPrefixes) =>
            Right(Some((Some(acc.getOrElse(Nil) :+ h), t)))
          case h :: t
              if h.hasValidScalacOptionDashes &&
              scalacAliasedOptions.exists(o => h.noDashPrefixes.startsWith(o + ":")) &&
              h.count(_ == ':') == 1 => Right(Some((Some(acc.getOrElse(Nil) :+ h), t)))
          case h :: t
              if h.hasValidScalacOptionDashes && scalacAliasedOptions.contains(h.noDashPrefixes) =>
            // check if the next scalac arg is a different option or a param to the current option
            val maybeOptionArg = t.headOption.filter(!_.startsWith("-"))
            // if it's a param, it'll be treated as such and considered already parsed
            val newTail = maybeOptionArg.map(_ => t.drop(1)).getOrElse(t)
            val newHead = List(h) ++ maybeOptionArg
            Right(Some((Some(acc.getOrElse(Nil) ++ newHead), newTail)))
          case _ => underlying.step(args, index, acc, formatter)
        }
      def get(acc: Option[List[String]], formatter: Formatter[Name]): Either[Error, List[String]] =
        Right(acc.getOrElse(Nil))
    }

  implicit lazy val parser: Parser[ScalacOptions] = {
    val baseParser                              = scalacOptionsArgument :: NilParser
    implicit val p: Parser[List[ArgFileOption]] = ArgFileOption.parser
    baseParser.addAll[List[ArgFileOption]].to[ScalacOptions]
  }

  implicit lazy val help: Help[ScalacOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalacOptions] = JsonCodecMaker.make
}

case class ArgFileOption(file: String) extends AnyVal

object ArgFileOption {
  val arg: Arg = Arg(
    name = Name("args-file"),
    valueDescription = Some(ValueDescription("@arguments-file")),
    helpMessage = Some(HelpMessage("File with scalac options.")),
    group = Some(Group("Scala")),
    origin = Some("ScalacOptions")
  )
  implicit lazy val parser: Parser[List[ArgFileOption]] = new Parser[List[ArgFileOption]] {
    type D = List[ArgFileOption] *: EmptyTuple

    override def withDefaultOrigin(origin: String): Parser[List[ArgFileOption]] = this

    override def init: D = Nil *: EmptyTuple

    override def step(args: List[String], index: Int, d: D, nameFormatter: Formatter[Name])
      : Either[(core.Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
      args match
        case head :: rest if head.startsWith("@") =>
          val newD = (ArgFileOption(head.stripPrefix("@")) :: d._1) *: EmptyTuple
          Right(Some(newD, arg, rest))
        case _ => Right(None)

    override def get(
      d: D,
      nameFormatter: Formatter[Name]
    ): Either[core.Error, List[ArgFileOption]] = Right(d.head)

    override def args: Seq[Arg] = Seq(arg)

  }
}
