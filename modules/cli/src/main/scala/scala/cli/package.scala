package scala

import coursier.core.Version

package object cli {
  extension (s: String) def coursierVersion: Version = Version(s)
}
