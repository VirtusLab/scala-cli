package scala.cli.internal

import packager.windows._

class GetImageResizer {
  def get(): ImageResizer =
    DefaultImageResizer
}
