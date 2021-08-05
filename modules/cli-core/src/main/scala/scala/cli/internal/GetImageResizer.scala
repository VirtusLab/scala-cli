package scala.cli.internal

import java.lang.management.ManagementFactory

import packager.windows._

class GetImageResizer {
  def get(): ImageResizer =
    DefaultImageResizer
}
