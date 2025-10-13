package scala.build

import coursier.version.Version

extension (s: String) def coursierVersion: Version = Version(s)
