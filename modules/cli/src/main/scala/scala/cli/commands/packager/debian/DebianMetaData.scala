package scala.cli.commands.packager.debian

case class DebianMetaData(
    debianInfo: DebianPackageInfo,
    architecture: String = "all",
    depends: Seq[String] = Seq.empty
) {

  def generateMetaContent(): String = {
    s"""Package: ${debianInfo.packageName}
    |Version: ${debianInfo.version}
    |Maintainer: ${debianInfo.maintainer}
    |Description: ${debianInfo.description}
    |Homepage: ${debianInfo.homepage}
    |Architecture: ${architecture}
    |""".stripMargin
  }

}
