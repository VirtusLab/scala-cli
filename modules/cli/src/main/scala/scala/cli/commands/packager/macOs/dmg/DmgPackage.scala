package scala.cli.commands.packager.macOs.dmg

import scala.build.Logger
import scala.cli.commands.packager.NativePackager
import scala.sys.process._

case class DmgPackage( sourceAppPath: os.Path, packageName: String)
  extends NativePackager {

  private val tempPackageName = s"$packageName-temp"
  private val basePath = sourceAppPath / os.RelPath("../")
  private val dmgAppPath = basePath / s"$packageName.app"
  private val contentPath = dmgAppPath / "Contents"
  private val macOsPath = contentPath / "MacOS"
  private val mountpointPath = basePath / "mountpoint"

  override def run(logger: Logger): Unit = {
    s"hdiutil create -megabytes 100  -fs HFS+ -volname $tempPackageName  $tempPackageName".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error creating disk image, exit code: $errorCode"
        )
    }

    createAppDirectory()
    createInfoPlist()

    s"hdiutil attach $tempPackageName.dmg -readwrite -mountpoint  mountpoint/".! match {
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

    s"hdiutil convert $tempPackageName.dmg -format UDZO -o $packageName.dmg".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error converting, exit code: $errorCode"
        )
    }

    os.remove(basePath / s"$tempPackageName.dmg")
    os.remove.all(dmgAppPath)
  }

  private def copyAppDirectory() = {
    os.copy(dmgAppPath, mountpointPath / s"$packageName.app")
    os.symlink(mountpointPath / "Applications", os.root / "Applications" )
  }

  private def createInfoPlist() = {
    val content = s"""<?xml version="1.0" encoding="UTF-8"?>
                    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                    |<plist version="1.0">
                    |<dict>
                    |	<key>CFBundleExecutable</key>
                    |	<string>$packageName</string>
                    |	<key>CFBundleIdentifier</key>
                    |	<string>com.example.$packageName</string>
                    |</dict>
                    |</plist>""".stripMargin

    os.write(contentPath / "Info.plist", content)
  }

  private def createAppDirectory() = {
    os.makeDir.all(macOsPath)
    os.copy(sourceAppPath, macOsPath / packageName)
  }
}