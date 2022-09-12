package scala.cli.internal

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.build.{Build, Inputs}
import scala.build.internal.Constants

object CachedBinary {

  final case class CacheData(changed: Boolean, projectSha: String)

  private def resolveProjectShaPath(workDir: os.Path) = workDir / ".project_sha"
  private def resolveOutputShaPath(workDir: os.Path)  = workDir / ".output_sha"

  private def fileSha(filePath: os.Path): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(os.read.bytes(filePath))

    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }

  private def hashResources(build: Build.Successful) = {
    def hashResourceDir(path: os.Path) =
      os.walk(path)
        .filter(os.isFile(_))
        .map { filePath =>
          val md = MessageDigest.getInstance("SHA-1")
          md.update(os.read.bytes(filePath))
          s"$filePath:" + new BigInteger(1, md.digest()).toString()
        }

    val classpathResourceDirsIt =
      build.options
        .classPathOptions
        .resourcesDir
        .flatMap(dir => hashResourceDir(dir))
        .iterator ++
        Iterator("\n")

    val projectResourceDirsIt = build.inputs.elements.iterator.flatMap {
      case elem: Inputs.OnDisk =>
        val content = elem match {
          case resDirInput: Inputs.ResourceDirectory =>
            hashResourceDir(resDirInput.path)
          case _ => List.empty
        }
        Iterator(elem.path.toString) ++ content.iterator ++ Iterator("\n")
      case _ =>
        Iterator.empty
    }

    (classpathResourceDirsIt ++ projectResourceDirsIt)
      .map(_.getBytes(StandardCharsets.UTF_8))
  }

  private def projectSha(build: Build.Successful, config: List[String]) = {
    val md      = MessageDigest.getInstance("SHA-1")
    val charset = StandardCharsets.UTF_8
    md.update(build.inputs.sourceHash().getBytes(charset))
    md.update("<resources>".getBytes())
    // Resource changes for SN require relinking, so they should also be hashed
    hashResources(build).foreach(md.update)
    md.update("</resources>".getBytes())
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
    workDir: os.Path,
    currentProjectSha: String
  ): Unit = {
    val projectShaPath = resolveProjectShaPath(workDir)
    os.write.over(projectShaPath, currentProjectSha, createFolders = true)

    val outputShaPath = resolveOutputShaPath(workDir)
    val sha           = fileSha(dest)
    os.write.over(outputShaPath, sha)
  }

  def getCacheData(
    build: Build.Successful,
    config: List[String],
    dest: os.Path,
    workDir: os.Path
  ): CacheData = {
    val projectShaPath = resolveProjectShaPath(workDir)
    val outputShaPath  = resolveOutputShaPath(workDir)

    val currentProjectSha = projectSha(build, config)
    val currentOutputSha  = if (os.exists(dest)) Some(fileSha(dest)) else None

    val previousProjectSha = if (os.exists(projectShaPath)) Some(os.read(projectShaPath)) else None
    val previousOutputSha  = if (os.exists(outputShaPath)) Some(os.read(outputShaPath)) else None

    val changed =
      !previousProjectSha.contains(currentProjectSha) ||
      previousOutputSha != currentOutputSha ||
      !os.exists(dest)

    CacheData(changed, currentProjectSha)
  }
}
