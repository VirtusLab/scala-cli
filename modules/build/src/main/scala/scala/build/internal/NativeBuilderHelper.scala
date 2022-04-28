package scala.build.internal

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.build.Build

object NativeBuilderHelper {

  case class SNCacheData(changed: Boolean, projectSha: String)

  private def resolveProjectShaPath(nativeWorkDir: os.Path) = nativeWorkDir / ".project_sha"
  private def resolveOutputShaPath(nativeWorkDir: os.Path)  = nativeWorkDir / ".output_sha"

  private def fileSha(filePath: os.Path): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(os.read.bytes(filePath))

    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }

  private def projectSha(build: Build.Successful, config: List[String]) = {
    val md      = MessageDigest.getInstance("SHA-1")
    val charset = StandardCharsets.UTF_8
    md.update(build.inputs.sourceHash().getBytes(charset))
    md.update(0: Byte)
    md.update("<config>".getBytes(charset))
    for (elem <- config) {
      md.update(elem.getBytes(charset))
      md.update(0: Byte)
    }
    md.update("</config>".getBytes(charset))
    md.update(Constants.version.getBytes)
    md.update(0: Byte)
    for (h <- build.options.hash) {
      md.update(h.getBytes(charset))
      md.update(0: Byte)
    }

    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }

  def updateProjectAndOutputSha(
    dest: os.Path,
    nativeWorkDir: os.Path,
    currentProjectSha: String
  ): Unit = {
    val projectShaPath = resolveProjectShaPath(nativeWorkDir)
    os.write.over(projectShaPath, currentProjectSha, createFolders = true)

    val outputShaPath = resolveOutputShaPath(nativeWorkDir)
    val sha           = fileSha(dest)
    os.write.over(outputShaPath, sha)
  }

  def getCacheData(
    build: Build.Successful,
    config: List[String],
    dest: os.Path,
    nativeWorkDir: os.Path
  ): SNCacheData = {
    val projectShaPath = resolveProjectShaPath(nativeWorkDir)
    val outputShaPath  = resolveOutputShaPath(nativeWorkDir)

    val currentProjectSha = projectSha(build, config)
    val currentOutputSha  = if (os.exists(dest)) Some(fileSha(dest)) else None

    val previousProjectSha = if (os.exists(projectShaPath)) Some(os.read(projectShaPath)) else None
    val previousOutputSha  = if (os.exists(outputShaPath)) Some(os.read(outputShaPath)) else None

    val changed =
      !previousProjectSha.contains(currentProjectSha) ||
      previousOutputSha != currentOutputSha ||
      !os.exists(dest)

    SNCacheData(changed, currentProjectSha)
  }
}
