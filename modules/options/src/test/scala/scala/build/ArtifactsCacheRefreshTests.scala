package scala.build

import com.eed3si9n.expecty.Expecty.expect
import com.sun.net.httpserver.HttpServer
import coursier.cache.FileCache
import coursier.core.{Module, ModuleName, Organization}
import coursier.maven.MavenRepository
import coursier.util.Task
import coursier.version.VersionConstraint

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt

class ArtifactsCacheRefreshTests extends munit.FunSuite {
  private val module    = Module(Organization("test.org"), ModuleName("lib"), Map.empty)
  private val moduleDir = "/maven/test/org/lib"

  private def metadata(versions: String*): String =
    s"""<metadata>
       |  <groupId>test.org</groupId>
       |  <artifactId>lib</artifactId>
       |  <versioning><versions>${versions.map(v =>
        s"<version>$v</version>"
      ).mkString}</versions></versioning>
       |</metadata>
       |""".stripMargin

  private def pom(version: String): String =
    s"""<project>
       |  <modelVersion>4.0.0</modelVersion>
       |  <groupId>test.org</groupId>
       |  <artifactId>lib</artifactId>
       |  <version>$version</version>
       |</project>
       |""".stripMargin

  /** A Maven repository served over HTTP, so that coursier caches what it fetches from it */
  private def withRepository[T](f: (String, TrieMap[String, String]) => T): T = {
    val responses = TrieMap.empty[String, String]
    val server    = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext(
      "/",
      exchange =>
        responses.get(exchange.getRequestURI.getPath) match {
          case Some(content) =>
            val bytes = content.getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.length)
            exchange.getResponseBody.write(bytes)
            exchange.close()
          case None =>
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
    )
    server.start()
    try f(s"http://127.0.0.1:${server.getAddress.getPort}/maven", responses)
    finally server.stop(0)
  }

  private def publish(responses: TrieMap[String, String], version: String): Unit = {
    responses(s"$moduleDir/$version/lib-$version.pom") = pom(version)
    responses(s"$moduleDir/$version/lib-$version.jar") = "jar"
  }

  test("a stale cached version listing is refreshed when fetching fails") {
    withRepository { (repoUrl, responses) =>
      val cacheDir = os.temp.dir(prefix = "scala-cli-cache-refresh-tests")
      try {
        val cache: FileCache[Task] =
          FileCache().withLocation(cacheDir.toString).withTtl(1.hour)
        def fetch(versionConstraint: String)(cache: FileCache[Task]) =
          coursier.Fetch(cache)
            .withRepositories(Seq(MavenRepository(repoUrl)))
            .addDependencies(coursier.Dependency(module, VersionConstraint(versionConstraint)))
            .either()
            .map(_.map(_.getName))

        publish(responses, "1.0.0")
        responses(s"$moduleDir/maven-metadata.xml") = metadata("1.0.0")
        // caches a version listing which only has 1.0.0
        expect(fetch("1.0.0+")(cache) == Right(Seq("lib-1.0.0.jar")))

        publish(responses, "1.1.0")
        responses(s"$moduleDir/maven-metadata.xml") = metadata("1.0.0", "1.1.0")
        // the cached version listing is stale
        expect(fetch("1.1.0+")(cache).isLeft)

        val res = Artifacts.withCacheRefreshOnFailure(cache, Logger.nop)(fetch("1.1.0+"))
        expect(res == Right(Seq("lib-1.1.0.jar")))
      }
      finally os.remove.all(cacheDir)
    }
  }
}
