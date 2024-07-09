package scala.cli.exportCmd

import java.nio.charset.StandardCharsets

import scala.build.options.{ConfigMonoid, Scope}
import scala.xml.{Elem, PrettyPrinter, XML}

final case class MavenProject(
  groupId: Option[String] = None,
  artifactId: Option[String] = None,
  version: Option[String] = None,
  plugins: Seq[MavenPlugin] = Nil,
  imports: Seq[String] = Nil,
  settings: Seq[Seq[String]] = Nil,
  dependencies: Seq[MavenLibraryDependency] = Nil,
  mainSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  testSources: Seq[(os.SubPath, String, Array[Byte])] = Nil
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
      plugins
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
  plugins: Seq[MavenPlugin]
  // properties: Seq[(String, String)] //todo: bring back only if it is needed
) {

//  private val propsElements = properties.map { case (key, value) =>
//    POMBuilderHelper.buildNode(key, value)
//  }

  def toXml: Elem =
    <project>
      <modelVersion>{model}</modelVersion>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>

      <!-- <properties>
        {propsElements}
      </properties> -->

      <dependencies>
        {dependencies.map(_.toXml)}
      </dependencies>
      <build>
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
  scope: String = Scope.Main.toString
) {
  def toXml: Elem =
    <dependency>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      <scope>{scope}</scope>
    </dependency>
}

final case class MavenPlugin(
  groupId: String,
  artifactId: String,
  version: String,
  jdk: String,
  configurationElems: Seq[Elem]
) {

  def toXml: Elem =
    <plugin>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      <configuration>
        {configurationElems}
      </configuration>
    </plugin>
}
