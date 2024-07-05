package scala.cli.exportCmd

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid

import scala.xml.{Elem, XML}

final case class MavenProject(
  plugins: Seq[MavenPlugin] = Nil,
  imports: Seq[String] = Nil,
  settings: Seq[Seq[String]] = Nil,
  dependencies: Seq[MavenLibraryDependency] = Nil,
  mainSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  testSources: Seq[(os.SubPath, String, Array[Byte])] = Nil
) extends Project {

  def +(other: MavenProject): MavenProject =
    MavenProject.monoid.orElse(this, other)

  def writeTo(dir: os.Path): Unit = {

    val nl                = System.lineSeparator()
    val charset           = StandardCharsets.UTF_8
    val buildMavenContent = "";

    os.write(
      dir / "pom.xml",
      buildMavenContent.getBytes(charset)
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
  dependencies: List[MavenLibraryDependency],
  plugins: List[MavenPlugin]
) {

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
  scope: String
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
  javacOpts: List[String],
  jdk: String
) {
  private def javaCompileOptionsXml: Elem = {
    val optionsXml = javacOpts.map { opt =>
      new Elem(
        null,
        "arg",
        scala.xml.Null,
        scala.xml.TopScope,
        minimizeEmpty = false,
        scala.xml.Text(opt)
      )
    }

    <compilerArgs>
        {optionsXml}
    </compilerArgs>

  }

  def toXml: Elem = {
    <plugin>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      <configuration>
        <source>{jdk}</source>
        <target>{jdk}</target>
        {javaCompileOptionsXml}
      </configuration>
    </plugin>
  }
}
