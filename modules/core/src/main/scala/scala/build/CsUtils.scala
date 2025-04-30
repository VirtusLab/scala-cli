package scala.build

import coursier.core.Version

extension (s: String) def coursierVersion: Version = Version(s)
