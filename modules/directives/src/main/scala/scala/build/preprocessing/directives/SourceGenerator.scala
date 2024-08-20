package scala.build.preprocessing.directives

// Virtuslab Processor
import com.virtuslab.using_directives.UsingDirectivesProcessor
import com.virtuslab.using_directives.custom.model.{
  BooleanValue,
  EmptyValue,
  StringValue,
  UsingDirectives,
  Value
}
import com.virtuslab.using_directives.custom.utils.ast._
import scala.jdk.CollectionConverters.*

import scala.cli.commands.SpecificationLevel
import scala.build.directives.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, SourceGeneratorOptions, GeneratorConfig}
import scala.build.options.GeneratorConfig
import scala.build.{Positioned, options}
import scala.build.directives.DirectiveValueParser.WithScopePath
import scala.util.matching.Regex
import java.nio.file.Paths
import scala.build.options.InternalOptions

@DirectiveGroupName("SourceGenerator")
@DirectivePrefix("sourceGenerator.")
@DirectiveUsage("//> using sourceGenerator", "`//> using sourceGenerator`")
@DirectiveDescription("Generate code using Source Generator")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class SourceGenerator(
  testy: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil),
  scripts: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil),
  excludeScripts: Option[Boolean] = None,
  inputDirectory: DirectiveValueParser.WithScopePath[Option[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(None),
  glob: Option[Positioned[String]] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    SourceGenerator.buildOptions(scripts, excludeScripts)
}

object SourceGenerator {
  val handler: DirectiveHandler[SourceGenerator] = DirectiveHandler.derive
  def buildOptions(
    scripts: DirectiveValueParser.WithScopePath[List[Positioned[String]]],
    excludeScripts: Option[Boolean]
  ): Either[BuildException, BuildOptions] = {
    val directiveProcessor = UsingDirectivesProcessor()
    val parsedDirectives = scripts.value
      .map(script => os.Path(script.value))
      .map(os.read(_))
      .map(_.toCharArray())
      .map(directiveProcessor.extract(_).asScala)
      .map(_.headOption)

    val scriptPaths = scripts.value
      .map(script =>
        os.Path(script.value)
      )

    def processDirectives(script: Option[UsingDirectives]) =
      script.toSeq.flatMap { directives =>
        def toStrictValue(value: UsingValue): Seq[Value[_]] = value match {
          case uvs: UsingValues   => uvs.values.asScala.toSeq.flatMap(toStrictValue)
          case el: EmptyLiteral   => Seq(EmptyValue(el))
          case sl: StringLiteral  => Seq(StringValue(sl.getValue(), sl))
          case bl: BooleanLiteral => Seq(BooleanValue(bl.getValue(), bl))
        }
        def toStrictDirective(ud: UsingDef) = StrictDirective(
          ud.getKey(),
          toStrictValue(ud.getValue()),
          ud.getPosition().getColumn()
        )

        directives.getAst match
          case uds: UsingDefs => uds.getUsingDefs.asScala.toSeq.map(toStrictDirective)
          case _              => Nil // There should be nothing else here other than UsingDefs
      }

    def replaceSpecialSyntax(directiveValue: String, path: os.Path): String = {
      val pattern = """(((?:\$)+)(\{\.\}))""".r
      pattern.replaceAllIn(
        directiveValue,
        (m: Regex.Match) => {
          val dollarSigns = m.group(2)
          val dollars     = "\\$" * (dollarSigns.length / 2)
          if (dollarSigns.length % 2 == 0)
            s"$dollars${m.group(3)}"
          else
            s"$dollars${path / os.up}"
        }
      )
    }
    val processedDirectives = parsedDirectives.map(processDirectives(_))

    val sourceGeneratorKeywords = Seq("inputDirectory", "glob")
    val sourceGeneratorDirectives = processedDirectives.map(directiveSeq =>
      directiveSeq.filter(rawDirective =>
        sourceGeneratorKeywords.exists(keyword => rawDirective.key.contains(keyword))
      )
    )

    sourceGeneratorDirectives.foreach { components =>
      if (components.length != components.distinct.length)
        throw new IllegalArgumentException(s"Duplicate elements found in sequence: $components")
    }

    val pathIterator = scriptPaths.iterator
    val generatorConfigs = sourceGeneratorDirectives.collect {
      case Seq(inputDir, glob) =>
        val relPath = pathIterator.next()
        GeneratorConfig(
          replaceSpecialSyntax(inputDir.values.mkString, relPath),
          List(glob.values.mkString),
          scripts.value(0).value,
          scripts.scopePath.subPath
        )
    }

    val excludedGeneratorPath = excludeScripts.match {
      case Some(true) => scripts.value
      case _          => List.empty[Positioned[String]]
    }

    Right(BuildOptions(
      sourceGeneratorOptions = SourceGeneratorOptions(generatorConfig = generatorConfigs),
      internal = InternalOptions(exclude = excludedGeneratorPath)
    ))
  }
}
