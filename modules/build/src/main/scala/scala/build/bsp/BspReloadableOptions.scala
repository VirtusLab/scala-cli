package scala.build.bsp

import bloop.rifle.BloopRifleConfig

import scala.build.Logger
import scala.build.options.BuildOptions

/** The options and configurations that may be picked up on a bsp workspace/reload request. They
  * don't take into account options from sources. The only two exceptions are the initial options in
  * BspImpl.run and in options used to launch new bloop in BspImpl.reloadBsp, which have the
  * [[bloopRifleConfig]] updated.
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
}
