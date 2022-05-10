package scala.build.blooprifle

import scala.concurrent.duration.Duration

final class FailedToStartServerException(timeoutOpt: Option[Duration] = None)
  extends Exception("Server didn't start" + timeoutOpt.fold("")(t => s" after $t"))
