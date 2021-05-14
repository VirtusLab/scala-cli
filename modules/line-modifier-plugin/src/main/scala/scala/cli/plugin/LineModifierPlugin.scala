package scala.cli.plugin

import scala.tools.nsc._
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.reporters.Reporter

// Initially extracted from https://github.com/com-lihaoyi/Ammonite/blob/0f0d597f04e62e86cbf76d3bd16deb6965331470/amm/compiler/src/main/scala/ammonite/compiler/AmmonitePlugin.scala,
// and tweaked so that it can handle multiple sources per compilation unit.

class LineModifierPlugin(val global: Global) extends Plugin { plugin =>

  val name = "linemodifier"
  val description = ""

  private var topWrapperLengths = Map.empty[String, Int]

  override def init(options: List[String], error: String => Unit): Boolean = {

    for (opt <- options)
      if (opt.startsWith("topWrapperLengths=")) {
        val input = opt.stripPrefix("topWrapperLengths=")
        topWrapperLengths ++= input
          .split(';')
          .filter(_.nonEmpty)
          .map(_.split("=", 2))
          .collect {
            case Array(path, len) => path -> len.toInt
          }
      } else
        sys.error(s"Unrecognized line modifier plugin option: $opt")

    true
  }

  val components: List[PluginComponent] =
    List(
      new PluginComponent {
        val global = plugin.global
        private lazy val corrector = LineModifierPlugin.lineNumberCorrector(global, topWrapperLengths)

        val runsAfter = List("parser")
        override val runsBefore = List("namer")
        val phaseName = "FixLineNumbers"

        def newPhase(prev: Phase): Phase =
          new global.GlobalPhase(prev) {
            def name = phaseName
            def apply(unit: global.CompilationUnit): Unit =
              unit.body = corrector(unit)
          }
      }
    )

}

object LineModifierPlugin {

  def lineNumberCorrector(global: Global, topWrapperLengths: Map[String, Int])
      : global.Transformer { def apply(unit: global.CompilationUnit): global.Tree } =
    new global.Transformer {

      import scala.reflect.internal.util._

      private var cache = Map.empty[String, Option[(BatchSourceFile, Int)]]

      override def transform(tree: global.Tree) = {

        val path = tree.pos.source.file.canonicalPath
        val paramsOpts = cache.getOrElse(
          path,
          {
            val value = topWrapperLengths.get(path).map { len =>
              val trimmedSource = new BatchSourceFile(
                tree.pos.source.file,
                tree.pos.source.content.drop(len)
              )
              (trimmedSource, len)
            }
            cache += path -> value
            value
          }
        )

        val transformedTree = super.transform(tree)

        for ((trimmedSource, topWrapperLen) <- paramsOpts) {
          val newPos = tree.pos match {
            case s: TransparentPosition if s.start >= topWrapperLen =>
                new TransparentPosition(
                  trimmedSource,
                  s.start - topWrapperLen,
                  s.point - topWrapperLen,
                  s.end - topWrapperLen
                )
            case s: RangePosition if s.start >= topWrapperLen =>
                new RangePosition(
                  trimmedSource,
                  s.start - topWrapperLen,
                  s.point - topWrapperLen,
                  s.end - topWrapperLen
                )
            case s: OffsetPosition if s.start >= topWrapperLen =>
                new OffsetPosition(trimmedSource, s.point - topWrapperLen)
            case s => s
          }

          transformedTree.pos = newPos
        }

        transformedTree
      }

      def apply(unit: global.CompilationUnit): global.Tree =
        transform(unit.body)
    }

}
