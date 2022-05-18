package scala.build.options.publish

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.{Constants, Ref}

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}
import scala.io.Codec
import scala.jdk.CollectionConverters._

sealed abstract class ComputeVersion extends Product with Serializable {
  def get(workspace: os.Path): Either[BuildException, String]
}

object ComputeVersion {

  final case class Command(command: Seq[String]) extends ComputeVersion {
    def get(workspace: os.Path): Either[BuildException, String] = {
      val res = os.proc(command).call(stdin = os.Inherit, cwd = workspace, check = false)
      if (res.exitCode == 0)
        Right(res.out.trim(Codec.default))
      else
        Left(new Command.ComputeVersionCommandError(command, res.exitCode))
    }
  }

  object Command {
    final class ComputeVersionCommandError(command: Seq[String], exitCode: Int)
        extends BuildException(
          s"Error running command ${command.mkString(" ")} (exit code: $exitCode)"
        )
  }

  final case class GitTag(
    repo: os.FilePath,
    dynVer: Boolean,
    defaultFirstVersion: String = "0.1.0-SNAPSHOT"
  ) extends ComputeVersion {
    import GitTag.GitTagError
    private def versionOf(tag: String): Option[String] = {
      val tag0 = tag.stripPrefix(Constants.R_TAGS)
      if (tag0.isEmpty) None
      else if (tag0.head.isDigit) Some(tag0)
      else if (tag0.length >= 2 && tag0(0) == 'v' && tag0(1).isDigit) Some(tag0.drop(1))
      else None
    }
    def get(workspace: os.Path): Either[BuildException, String] = {
      val repo0 = repo.resolveFrom(workspace)
      if (os.exists(repo0 / ".git")) {
        val git     = Git.open(repo0.toIO)
        val hasHead = git.getRepository.resolve(Constants.HEAD) != null
        if (hasHead) {
          val (lastTagOpt, lastStableTagOpt) = {
            val tagMap = git.tagList()
              .call()
              .asScala
              .iterator
              .flatMap { tag =>
                Option(git.getRepository.getRefDatabase.peel(tag).getPeeledObjectId)
                  .orElse(Option(tag.getObjectId))
                  .orElse(Option(tag.getPeeledObjectId))
                  .iterator
                  .map(id => (id.name, tag))
              }
              .toMap
            val tagsIt = git.log()
              .call()
              .asScala
              .iterator
              .flatMap(c => tagMap.get(c.name()).iterator)
              .flatMap(r => versionOf(r.getName).map((r, _)).iterator)
              .scanLeft((Option.empty[(Ref, String)], Option.empty[(Ref, String)])) {
                case ((acc, stableAcc), v @ (_, name)) =>
                  val acc0 = acc.orElse(Some(v))
                  val stableAcc0 = stableAcc.orElse {
                    if (name.forall(c => c == '.' || c.isDigit)) Some(v)
                    else None
                  }
                  (acc0, stableAcc0)
              }
            var lastTagOpt0       = Option.empty[(Ref, String)]
            var lastStableTagOpt0 = Option.empty[(Ref, String)]
            while (tagsIt.hasNext && (lastTagOpt0.isEmpty || lastStableTagOpt0.isEmpty)) {
              val v = tagsIt.next()
              if (lastTagOpt0.isEmpty)
                lastTagOpt0 = v._1
              if (lastStableTagOpt0.isEmpty)
                lastStableTagOpt0 = v._2
            }
            (lastTagOpt0, lastStableTagOpt0)
          }
          val headCommit = git.log().call().asScala.iterator.next()

          (lastTagOpt, lastStableTagOpt) match {
            case (None, _) =>
              Right(defaultFirstVersion)
            case (Some((tag, name)), _)
                if Option(git.getRepository.getRefDatabase.peel(tag).getPeeledObjectId)
                  .exists(_.name == headCommit.name) ||
                Option(tag.getObjectId).exists(_.name == headCommit.name) =>
              Right(name)
            case (Some((tag, _)), _) if dynVer =>
              val tagOrNull = git.describe()
                .setMatch("v[0-9]*", "[0-9]*")
                .setTags(true)
                .setTarget(headCommit)
                .call()
              Option(tagOrNull) match {
                case None =>
                  Left(new GitTagError(
                    s"Unexpected error when running git describe from Git repository $repo0 (git describe doesn't find back tag $tag)"
                  ))
                case Some(tag) =>
                  versionOf(tag).map(_ + "-SNAPSHOT").toRight(
                    new GitTagError(
                      s"Unexpected error when running git describe from Git repository $repo0 (git describe-provided tag $tag doesn't have the expected shape)"
                    )
                  )
              }
            case (Some(_), None) =>
              Left(new GitTagError(s"No stable tag found in Git repository $repo0"))
            case (_, Some((tag, name))) =>
              val idx = name.lastIndexOf('.')
              if (
                idx >= 0 && idx < name.length - 1 && name.iterator.drop(idx + 1).forall(_.isDigit)
              )
                Right(name.take(idx + 1) + (name.drop(idx + 1).toInt + 1).toString + "-SNAPSHOT")
              else
                Left(new GitTagError(
                  s"Don't know how to bump version in tag $tag in Git repository $repo0"
                ))
          }
        }
        else
          Right(defaultFirstVersion)
      }
      else
        Left(new GitTagError(s"$repo0 doesn't look like a Git repository"))
    }
  }
  object GitTag {
    final class GitTagError(message: String) extends BuildException(message)
  }

  private lazy val commandCodec: JsonValueCodec[List[String]] =
    JsonCodecMaker.make

  def parse(input: Positioned[String]): Either[BuildException, ComputeVersion] =
    if (input.value == "git" || input.value == "git:tag")
      Right(ComputeVersion.GitTag(os.rel, dynVer = false))
    else if (input.value.startsWith("git:tag:"))
      Right(ComputeVersion.GitTag(os.FilePath(input.value.stripPrefix("git:tag:")), dynVer = false))
    else if (input.value == "git:dynver")
      Right(ComputeVersion.GitTag(os.rel, dynVer = true))
    else if (input.value.startsWith("git:dynver:"))
      Right(ComputeVersion.GitTag(
        os.FilePath(input.value.stripPrefix("git:dynver:")),
        dynVer = true
      ))
    else if (input.value.startsWith("command:["))
      try {
        val command = readFromString(input.value.stripPrefix("command:"))(commandCodec)
        Right(ComputeVersion.Command(command))
      }
      catch {
        case e: JsonReaderException =>
          Left(
            new MalformedInputError(
              "compute-version",
              input.value,
              "git|git:tag|command:…",
              input.positions,
              cause = Some(e)
            )
          )
      }
    else if (input.value.startsWith("command:")) {
      val command = input.value.stripPrefix("command:").split("\\s+").toSeq
      Right(ComputeVersion.Command(command))
    }
    else
      Left(
        new MalformedInputError(
          "compute-version",
          input.value,
          "git|git:tag|command:…",
          input.positions
        )
      )
}
