package scala.build.bsp

import scala.build.Logger
import scala.build.blooprifle.BloopRifleConfig
import scala.build.options.BuildOptions

/** The options and configurations that may be picked up on a bsp workspace/reload request.
  *
  * @param buildOptions
  *   passed options for building sources
  * @param bloopRifleConfig
  *   configuration for bloop-rifle
  * @param logger
  *   logger
  * @param verbosity
  *   the verbosity of logs
  */
case class BspReloadableOptions(
  buildOptions: BuildOptions,
  bloopRifleConfig: BloopRifleConfig,
  logger: Logger,
  verbosity: Int
)

object BspReloadableOptions {
  class Reference(getReloaded: () => BspReloadableOptions) {
    @volatile private var ref: BspReloadableOptions = getReloaded()
    def get: BspReloadableOptions                   = ref
    def reload(): Unit                              = ref = getReloaded()
  }
  object Reference {
    def apply(getReloaded: () => BspReloadableOptions): Reference = new Reference(getReloaded)
  }
}
