package scala.cli.exportCmd

import os.RelPath

import java.nio.charset.StandardCharsets

import scala.build.options.{ConfigMonoid, Scope}
import scala.xml.{Elem, NodeSeq, PrettyPrinter, XML}

final case class MavenProject(
  groupId: Option[String] = None,
  artifactId: Option[String] = None,
  version: Option[String] = None,
  plugins: Seq[MavenPlugin] = Nil,
  imports: Seq[String] = Nil,
  settings: Seq[Seq[String]] = Nil,
  dependencies: Seq[MavenLibraryDependency] = Nil,
  mainSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  testSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  resourceDirectories: Seq[String] = Nil
  // properties: Seq[(String, String)] = Nil // using Seq[Tuple] since derive was failing for Map
) extends Project {

  def +(other: MavenProject): MavenProject =
    MavenProject.monoid.orElse(this, other)

  def writeTo(dir: os.Path): Unit = {

    val nl      = System.lineSeparator()
    val charset = StandardCharsets.UTF_8

    val buildMavenContent = MavenModel(
      "4.0.0",
      groupId.getOrElse("groupId"),
      artifactId.getOrElse("artifactId"),
      version.getOrElse("0.1-SNAPSHOT"),
      dependencies,
      plugins,
      resourceDirectories
      // properties
    )

    val prettyPrinter = new PrettyPrinter(width = 80, step = 2)
    val formattedXml  = prettyPrinter.format(buildMavenContent.toXml)

    os.write(
      dir / "pom.xml",
      formattedXml.getBytes(charset)
    )

    for ((path, language, content) <- mainSources) {
      val path0 = dir / "src" / "main" / language / path
      os.write(path0, content, createFolders = true)
    }
    for ((path, language, content) <- testSources) {
      val path0 = dir / "src" / "test" / language / path
      os.write(path0, content, createFolders = true)
    }

  }
}

object MavenProject {
  implicit val monoid: ConfigMonoid[MavenProject] = ConfigMonoid.derive
}

final case class MavenModel(
  model: String,
  groupId: String,
  artifactId: String,
  version: String,
  dependencies: Seq[MavenLibraryDependency],
  plugins: Seq[MavenPlugin],
  resourceDirectories: Seq[String]
  // properties: Seq[(String, String)] //todo: bring back only if it is needed
) {

  private def resourceNodes: NodeSeq =
    if (resourceDirectories.isEmpty)
      NodeSeq.Empty
    else {
      val resourceNodes = resourceDirectories.map { path =>
        <resource>
          <directory>
            {path}
          </directory>
        </resource>
      }
      <resources>
        {resourceNodes}
      </resources>
    }

  def toXml: Elem =
    <project>
      <modelVersion>{model}</modelVersion>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>

      <dependencies>
        {dependencies.map(_.toXml)}
      </dependencies>
      <build>
        {resourceNodes}
        <plugins>
          {plugins.map(_.toXml)}
        </plugins>
      </build>
    </project>
}

final case class MavenLibraryDependency(
  groupId: String,
  artifactId: String,
  version: String,
  scope: MavenScopes
) {

  private val scopeParam =
    if scope == MavenScopes.Main then scala.xml.Null else <scope>{scope.name}</scope>

  def toXml: Elem =
    <dependency>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      {scopeParam}
    </dependency>
}

final case class MavenPlugin(
  groupId: String,
  artifactId: String,
  version: String,
  jdk: String,
  additionalNode: Elem
) {

  def toXml: Elem =
    <plugin>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      {additionalNode}
    </plugin>
}
