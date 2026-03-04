#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using dep org.scala-lang.modules::scala-xml:2.4.0
//> using options -Werror -Wunused:all
// adapted from https://github.com/vic/mill-test-junit-report
import java.io.File
import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec
import java.nio.file.Paths
import scala.util.Try

case class Trace(declaringClass: String, methodName: String, fileName: String, lineNumber: Int) {
  override def toString: String = s"$declaringClass.$methodName($fileName:$lineNumber)"
}

case class Failure(name: String, message: String, trace: Seq[Trace])

case class Test(
  fullyQualifiedName: String,
  selector: String,
  duration: Double,
  failure: Option[Failure]
)

@tailrec
def findFiles(paths: Seq[os.Path], result: Seq[os.Path] = Nil): Seq[os.Path] =
  paths match
    case Nil          => result
    case head :: tail =>
      val newFiles =
        if head.segments.contains("test") && head.last.endsWith(".dest") && os.isDir(head) then
          os.list(head).filter(f => f.last == "out.json").toList
        else Seq.empty
      val newDirs = os.list(head).filter(p => os.isDir(p)).toList
      findFiles(tail ++ newDirs, result ++ newFiles)

extension (s: String)
  def toNormalisedPath: os.Path = if Paths.get(s).isAbsolute then os.Path(s) else os.Path(s, os.pwd)

def printUsageMessage(): Unit = println("Usage: generate-junit-reports <id> <name> <into> <path>")
if args.length != 4 then {
  println(s"Error: provided too few arguments: ${args.length}")
  printUsageMessage()
  System.exit(1)
}

val id: String   = args(0)
val name: String = args(1)

if new File(args(2)).exists() then {
  println(s"Error: specified output path already exists: ${args(2)}")
  System.exit(1)
}
val into = args(2).toNormalisedPath

val pathArg           = args(3)
val rootPath: os.Path =
  if Paths.get(pathArg).isAbsolute then os.Path(pathArg) else os.Path(pathArg, os.pwd)
if !os.isDir(rootPath) then {
  println(s"The path provided is not a directory: $pathArg")
  System.exit(1)
}
val reports: Seq[os.Path] = findFiles(Seq(rootPath))
println(s"Found ${reports.length} mill json reports:")
println(reports.mkString("\n"))
if reports.isEmpty then println("Warn: no reports found!")
println("Reading reports...")
val tests: Seq[Test] = reports.map(x => ujson.read(x.toNIO)).flatMap { json =>
  json(1).value.asInstanceOf[ArrayBuffer[ujson.Obj]].map { test =>
    Test(
      fullyQualifiedName = test("fullyQualifiedName").str,
      selector = test("selector").str,
      duration = test("duration").num / 1000.0,
      failure = test("status").str match {
        case "Failure" => Some(Failure(
            name = test("exceptionName")(0).str,
            message = test("exceptionMsg")(0).str,
            trace = test("exceptionTrace")(0).arr.map { st =>
              val declaringClass = st("declaringClass").str
              val methodName     = st("methodName").str
              val fileName       = st("fileName")(0).str
              val lineNumber     = st("lineNumber").num.toInt
              Trace(declaringClass, methodName, fileName, lineNumber)
            }.toList
          ))
        case _ => None
      }
    )
  }
}
println(s"Found ${tests.length} tests.")
if tests.isEmpty then println("Warn: no tests found!")
println("Generating JUnit XML report...")
val suites = tests.groupBy(_.fullyQualifiedName).map { case (suit, tests) =>
  val testcases = tests.map { test =>
    <testcase id={test.selector} classname={test.fullyQualifiedName} name={
      test.selector.substring(test.fullyQualifiedName.length)
    } time={test.duration.toString}>
        {
      test.failure.map { failure =>
        val maybeTrace = Try(failure.trace(1)).toOption
        val fileName   = maybeTrace.map(_.fileName).getOrElse("unknown")
        val lineNumber = maybeTrace.map(_.lineNumber).getOrElse(-1)
        <failure message={failure.message} type="ERROR">
            ERROR: {failure.message}
            Category: {failure.name}
            File: {fileName}
            Line: {lineNumber}
          </failure>
      }.orNull
    }
        {
      test.failure.map { failure =>
        <system-err>{
          failure.trace.mkString(s"${failure.name}: ${failure.message}", "\n    at ", "")
        }</system-err>
      }.orNull
    }
      </testcase>
  }

  <testsuite id={suit} name={suit} tests={tests.length.toString} failures={
    tests.count(_.failure.isDefined).toString
  } time={tests.map(_.duration).sum.toString}>
      {testcases}
    </testsuite>
}

val n = <testsuites id={id} name={name} tests={tests.length.toString} failures={
  tests.count(_.failure.isDefined).toString
} time={tests.map(_.duration).sum.toString}>
  {suites}
</testsuites>
val prettyXmlPrinter = new scala.xml.PrettyPrinter(80, 2)
val xmlToSave        = scala.xml.XML.loadString(prettyXmlPrinter.format(n))
scala.xml.XML.save(filename = into.toString(), node = xmlToSave, xmlDecl = true)
println(s"Generated report at: $into")
