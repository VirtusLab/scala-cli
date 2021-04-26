package scala.cli

import java.nio.file.Paths
import ammonite.util.Printer
import ammonite.interp.DependencyLoader
import ammonite.runtime.Storage
import ammonite.interp.script.ScriptProcessor
import ammonite.compiler.Parsers
import ammonite.util.Util
import ammonite.util.Name
import ammonite.compiler.iface.CodeWrapper

final case class Sources(
  paths: Seq[String],
  inMemory: Seq[(String, String)],
  mainClass: Option[String],
  dependencies: Seq[coursierapi.Dependency]
) {

  def generateSources(generatedSrcRoot: os.Path): Seq[os.Path] = {
    val generated =
      for ((className, code) <- inMemory) yield {
        val components = className.split('.')
        val pkg = components.init
        val simpleName = components.last
        val srcDest = os.rel / "main" / "scala" / pkg / s"$simpleName.scala"
        os.write.over(generatedSrcRoot / srcDest, code.getBytes("UTF-8"), createFolders = true)
        srcDest
      }

    val generatedSet = generated.toSet
    if (os.isDir(generatedSrcRoot))
      os.walk(generatedSrcRoot)
        .filter(os.isFile(_))
        .filter(p => !generatedSet(p.relativeTo(generatedSrcRoot)))
        .foreach(os.remove(_))

    generated.map(generatedSrcRoot / _)
  }

  def checkPlatformDependencies(platformSuffix: String, scalaVersion: String, scalaBinaryVersion: String): Sources = {

    def updateDependency(dep: coursierapi.Dependency): coursierapi.Dependency = {
      val substitute =
        if (dep.getModule.getName.endsWith("_" + scalaVersion)) {
          val mod = coursierapi.Module.of(dep.getModule.getOrganization, ???, dep.getModule.getAttributes)
          coursierapi.Dependency.of(dep).withModule(mod)
        }
      ???

    }

    this
  }
}

object Sources {

  private def check(path: os.Path): Unit = {
    System.err.println(s"check($path)")
    val content = os.read(path)

    import fastparse._
    import scalaparse._
    import scala.cli.internal.ScalaParse._

    val res = parse(content, Header(_))
    pprint.log(res)

    val indices = res.fold((_, _, _) => Nil, (value, _) => value)

    for ((start, end) <- indices) {
      val code = content.substring(start, end)
        // .trim // meh
      pprint.log(code)
      val importRes = parse(code, ImportSplitter(_))
      pprint.log(importRes)
      if (importRes.isSuccess) {
        val trees = importRes.get.value
        for (tree <- trees) {
          val subImportStr = code.substring(tree.start, tree.end)
          pprint.log(subImportStr)
        }
      }
    }

    val endIdx = res.fold((_, idx, _) => Left(idx), (_, idx) => Right(idx))
    pprint.log(endIdx)

    endIdx match {
      case Left(failingIdx) =>
        val newCode = content.take(failingIdx)
        pprint.log(newCode)
        val res1 = parse(newCode, Header(_))
        pprint.log(res1)
      case Right(endIdx0) =>
        val retainedCode = content.take(endIdx0)
        pprint.log(retainedCode)
    }
  }

  def apply(
    root: os.Path,
    inputs: Inputs,
    codeWrapper: CodeWrapper,
    scalaVersion: String
  ): Sources = {

    val printer = Printer(System.out, System.err, System.out, println, println, println)

    val dependencyLoader = new DependencyLoader(
      printer,
      Storage.InMemory(),
      Nil,
      verboseOutput = false
    )

    val processor = ScriptProcessor(
      Parsers,
      codeWrapper,
      dependencyLoader,
      Nil
    )

    def scriptData(path: String) = {

      val scriptPath = os.Path(root.toNIO.resolve(path).toAbsolutePath.normalize)

      val codeSource = {
        val (pkg, wrapper) = Util.pathToPackageWrapper(Nil, scriptPath.relativeTo(root))
        Util.CodeSource(
          wrapper,
          pkg,
          Seq(Name("ammonite"), Name("$file")), // kind of meh, required by an assertion in the CodeSource body
          Some(scriptPath)
        )
      }

      // check(scriptPath)

      val script0 = processor.load(os.read(scriptPath), codeSource)
      val wrapperName = codeSource.wrapperName
      val block = script0.blocks.head
      // TODO Add support for platform dependencies
      // TODO Remove the removeSpuriousSuffixes stuff once we can pass scalaVersions to ScriptProcessor
      val scriptDependencies = script0.dependencies.dependencies
        .map(removeSpuriousSuffixes(scala.util.Properties.versionNumberString, scalaVersion))
      val (code, _, _) = codeWrapper.wrapCode(
        codeSource,
        wrapperName,
        block.statements.mkString(""),
        "",
        script0.dependencyImports,
        "",
        markScript = true
      )
      ScriptData(wrapperName.raw, code, scriptDependencies)
    }

    // TODO Get dependencies from .scala files

    val fromDirectories = inputs.elements
      .collect {
        case d: Inputs.Directory =>
          val dir = os.Path(Paths.get(d.path).normalize.toAbsolutePath)
          os.walk.stream(dir)
            .collect {
              case p if os.isFile(p) && (p.last.endsWith(".scala") || p.last.endsWith(".sc")) =>
                if (p.last.endsWith(".scala"))
                  Inputs.ScalaFile(p.relativeTo(root).toString)
                else
                  Inputs.Script(p.relativeTo(root).toString)
            }
            .toVector
      }
      .flatten

    val scalaFilePaths = (inputs.elements.iterator ++ fromDirectories.iterator)
      .collect {
        case f: Inputs.ScalaFile => f.path
      }
      .toVector

    val headScriptDataOpt = inputs.elements.headOption.collect {
      case s: Inputs.Script => scriptData(s.path)
    }
    def otherScriptsData = (inputs.elements.iterator.drop(1) ++ fromDirectories.iterator).collect {
      case s: Inputs.Script => scriptData(s.path)
    }
    val allScriptData = (headScriptDataOpt.iterator ++ otherScriptsData).toVector

    Sources(
      paths = scalaFilePaths,
      inMemory = allScriptData.map(script => script.className -> script.code),
      mainClass = headScriptDataOpt.map(_.className),
      dependencies = allScriptData.flatMap(_.dependencies).distinct
    )
  }

  private final case class ScriptData(className: String, code: String, dependencies: Seq[coursierapi.Dependency])

  private def mapBinarySuffix(from: String, to: String): coursierapi.Dependency => coursierapi.Dependency =
    if (from == to) identity
    else
      dep =>
        if (dep.getModule.getName.endsWith(from)) {
          val origModule = dep.getModule
          val updatedModule = coursierapi.Module.of(origModule.getOrganization, origModule.getName.stripSuffix(from) + to)
          dep.withModule(updatedModule)
        } else
          dep
  private def removeSpuriousSuffixes(from: String, to: String): coursierapi.Dependency => coursierapi.Dependency =
    if (from == to) identity
    else {
      def binaryVersion(v: String) = v.split('.').take(2).mkString(".") // meh
      val f1 = mapBinarySuffix("_" + from, "_" + to)
      val f2 = mapBinarySuffix("_" + binaryVersion(from), "_" + binaryVersion(to))
      dep => f1(f2(dep))
    }
}
