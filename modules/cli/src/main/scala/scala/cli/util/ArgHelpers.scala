package scala.cli.util

import caseapp.core.Arg
import caseapp.core.help.HelpFormat

import scala.cli.ScalaCli.allowRestrictedFeatures
import scala.cli.commands.{SpecificationLevel, tags}

object ArgHelpers {
  extension (arg: Arg) {
    def isExperimentalOrRestricted: Boolean =
      arg.tags.exists(_.name == tags.restricted) || arg.tags.exists(_.name == tags.experimental)

    def isSupported: Boolean = allowRestrictedFeatures || !arg.isExperimentalOrRestricted
    def isImportant: Boolean = arg.tags.exists(_.name == tags.important)

    def isMust: Boolean = arg.tags.exists(_.name == tags.must)

    def level: SpecificationLevel = arg.tags
      .flatMap(t => tags.levelFor(t.name))
      .headOption
      .getOrElse(SpecificationLevel.IMPLEMENTATION)
  }

  extension (helpFormat: HelpFormat) {
    def withPrimaryGroup(primaryGroup: String): HelpFormat = {
      val oldSortedGroups = helpFormat.sortedGroups.getOrElse(Seq.empty)
      helpFormat.copy(sortedGroups = Some(oldSortedGroups.prepended(primaryGroup)))
    }
  }
}
