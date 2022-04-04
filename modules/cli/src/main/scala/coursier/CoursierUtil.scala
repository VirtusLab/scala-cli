package coursier

import coursier.util.WebPage

case object CoursierUtil {

  def rawVersions(repoUrl: String, page: String) = WebPage.listDirectories(repoUrl, page)

}
