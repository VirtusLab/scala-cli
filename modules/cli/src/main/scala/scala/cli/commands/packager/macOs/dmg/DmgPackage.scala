package scala.cli.commands.packager.macOs.dmg

import scala.build.Logger
import scala.cli.commands.packager.macOs.MacOsNativePackager
import scala.sys.process._

case class DmgPackage( sourceAppPath: os.Path, packageName: String)
  extends MacOsNativePackager {

  private val tmpPackageName = s"$packageName-tmp"
  private val mountpointPath = basePath / "mountpoint"

  override def run(logger: Logger): Unit = {
    s"hdiutil create -megabytes 100  -fs HFS+ -volname $tmpPackageName  $tmpPackageName".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error creating disk image, exit code: $errorCode"
        )
    }

    createAppDirectory()
    createInfoPlist()

    s"hdiutil attach $tmpPackageName.dmg -readwrite -mountpoint  mountpoint/".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error attaching mountpoint, exit code: $errorCode"
        )
    }

    copyAppDirectory()

    s"hdiutil detach mountpoint/".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error detaching mountpoint, exit code: $errorCode"
        )
    }

    s"hdiutil convert $tmpPackageName.dmg -format UDZO -o $packageName.dmg".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error converting, exit code: $errorCode"
        )
    }

    postInstallClean()
  }

  private def postInstallClean() = {
    os.remove(basePath / s"$tmpPackageName.dmg")
    os.remove.all(macOsAppPath)
  }

  private def copyAppDirectory() = {
    os.copy(macOsAppPath, mountpointPath / s"$packageName.app")
    os.symlink(mountpointPath / "Applications", os.root / "Applications" )
  }

}