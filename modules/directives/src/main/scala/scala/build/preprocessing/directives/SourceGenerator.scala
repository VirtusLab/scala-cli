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
  glob: Option[Positioned[String]] = None,
  commandProcessor: DirectiveValueParser.WithScopePath[Option[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(None),
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    // println(s"ScopePath of Scripts: ${scripts.scopePath}")
    // println(s"Values of Scripts: ${scripts.value(0).value}")
    // println(s"Values of InputDir: ${inputDirectory.value}")
    SourceGenerator.buildOptions(testy,scripts)
}

object SourceGenerator {
  val handler: DirectiveHandler[SourceGenerator] = DirectiveHandler.derive
  def buildOptions(
    sourceGenerator: DirectiveValueParser.WithScopePath[List[Positioned[String]]],
    scripts: DirectiveValueParser.WithScopePath[List[Positioned[String]]]
  ): Either[BuildException, BuildOptions] = {
    val proc = UsingDirectivesProcessor()
    val scriptConvert = scripts.value
      .map(script => os.Path(script.value))
      .map(os.read(_))
      .map(_.toCharArray())
      .map(proc.extract(_).asScala)
      .map(_.headOption)

    // println(scriptConvert.size)

    def modify(script: Option[UsingDirectives]) = {
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

        // println(directives.getAst())

        directives.getAst match
          case uds: UsingDefs => uds.getUsingDefs.asScala.toSeq.map(toStrictDirective)
          case _              => Nil // There should be nothing else here other than UsingDefs
      }
    }

    val strictDirectives = scriptConvert
    .map(modify(_))
    .map(_.take(3))

    // println(scripts.scopePath.subPath)
    // strictDirectives.map(f => f.map(w => println(w)))

    val directive = strictDirectives.collect {
      case Seq(inputDir, glob, processor) => 
        GeneratorConfig(inputDir.values.mkString, List(glob.values.mkString), processor.values.mkString.split(" ").toList,scripts.scopePath.subPath)
    }
    println(directive.size)

    // val sourceGenValue = sourceGenerator.value
    // sourceGenValue
    //   .map(config => GeneratorConfig.parse(config, sourceGenerator.scopePath.subPath))
    //   .sequence
    //   .left.map(CompositeBuildException(_))
    //   .map { configs =>
    //     BuildOptions(sourceGeneratorOptions =
    //       SourceGeneratorOptions(generatorConfig = configs)
    //     )
    //   }
    
    // directive.map { f => println(f)}

    Right(BuildOptions(sourceGeneratorOptions = SourceGeneratorOptions(generatorConfig = directive)))
  }
}
