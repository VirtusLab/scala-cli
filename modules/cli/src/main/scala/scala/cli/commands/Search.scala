package scala.cli.commands

import caseapp._

import scala.cli.CurrentParams

import scala.io.StdIn.readLine
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

object Search extends ScalaCommand[SearchOptions] {
  override def group = "Main"

  implicit val searchResultCodec: JsonValueCodec[SearchResult] = JsonCodecMaker.make
  implicit val searchResultsCodec: JsonValueCodec[List[SearchResult]] = JsonCodecMaker.make
  implicit val projectCodec: JsonValueCodec[Project] = JsonCodecMaker.make

  case class SearchResult(
    organization: String,
    repository: String,
    artifacts: List[String],
  )

  implicit class SearchResultOps(r: SearchResult) {
    def enumerate(index: Int, count: Int): String = {
      val artifactsLines = limitLine(r.artifacts, "  ").mkString("\n")
      val idx = index.toString.padLeftTo(count.toString.size, ' ')

      s"""$idx: ${r.organization}/${r.repository}
         |$artifactsLines""".stripMargin
    }

    // Scaladex returns related searches, sometimes they don't contain `q`
    def getStrict(q: String): Option[SearchResult] =
      if (r.organization.contains(q) || r.repository.contains(q))
        Some(r)
      else {
        val strictA = r.artifacts.filter(_.contains(q))
        strictA match {
          case Nil => None
          case sa => Some(r.copy(artifacts = sa))
        }
      }
  }

  case class Project(
    groupId: String,
    artifactId: String,
    version: String,
  )

  implicit class ProjectOps(project: Project) {
    def getImportString = {
      import project._
      s"import $$ivy.`$groupId:$artifactId:$version`"
    }
  }

  def limitLine(strs: List[String], lineStart: String = "", size: Int = 80) = {
    def loop(strs: List[String], lastLine: String, allLines: List[String]): List[String] =
      strs match {
        case Nil => (lastLine :: allLines).reverse
        case str :: rest =>
          val newSize = lastLine.size + 2 + str.size + 1
          if (newSize > size)
            loop(rest, lineStart + str, (lastLine + ",") :: allLines)
          else {
            val newLine =
              if (lastLine == lineStart)
                lastLine + str
              else
                lastLine + ", " + str
            loop(rest, newLine, allLines)
          }
      }

    loop(strs, lineStart, Nil)
  }

  implicit class StringOps(s: String) {
    def padLeftTo(len: Int, elem: Char): String =
      s.reverse.padTo(len, elem).reverse

    def enumerate(index: Int, count: Int): String = {
      val idx = index.toString.padLeftTo(count.toString.size, ' ')

      s"""$idx: $s"""
    }
  }

  def fetch[A: JsonValueCodec](url: String): A = {
    println(s"Fetching from $url...")
    val text = scala.cli.internal.ProcUtil.downloadFile(url)

    readFromArray[A](text.getBytes("UTF-8"))
  }

  def search(q: String, target: String, scalaBinary: String): List[SearchResult] = {
    val url = s"https://index.scala-lang.org/api/search?q=$q&target=$target&scalaVersion=$scalaBinary"
    fetch[List[SearchResult]](url)
  }

  def getProject(organization: String, repository: String, artifact: String, target: String, scalaBinary: String): Project = {
    val url = s"https://index.scala-lang.org/api/project?organization=$organization&repository=$repository&artifact=$artifact&target=$target&scalaVersion=$scalaBinary"
    fetch[Project](url)
  }

  def run(options: SearchOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity

    val query = options.query.getOrElse("")
    val target = options.target.getOrElse("")
    val scalaBinary = options.scalaBinary.getOrElse("")
    val strict = options.strict.getOrElse(true)

    val searchResults: List[SearchResult] = {
      val searchResults = search(query, target, scalaBinary)
      if (strict) searchResults.flatMap(_.getStrict(query))
      else searchResults
    }

    println("Found these artifacts:")
    searchResults.zipWithIndex.map {
      case (r, i) => r.enumerate(i + 1, searchResults.size)
    }.foreach(println)

    println()
    print("Choose the repository: ")
    val repositoryIndex = {
      val idx = readLine()
      idx.toInt - 1
    }

    val organization = searchResults(repositoryIndex).organization
    val repository = searchResults(repositoryIndex).repository

    println()
    println(s"Artifacts in $organization/$repository:")
    val artifacts = searchResults(repositoryIndex).artifacts
    artifacts.zipWithIndex.map {
      case (a, i) => a.enumerate(i + 1, artifacts.size)
    }.foreach(println)

    println()
    print("Choose the artifact: ")
    val artifactIndex = {
      val idx = readLine()
      idx.toInt - 1
    }

    val artifact = artifacts(artifactIndex)
    val projectJson = getProject(organization, repository, artifact, target, scalaBinary)

    println()
    println("Use this import:")
    println(projectJson.getImportString)
    println()
  }
}
