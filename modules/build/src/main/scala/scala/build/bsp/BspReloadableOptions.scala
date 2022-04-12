package scala.build.bsp

import java.util.concurrent.atomic.AtomicReference

import scala.build.Logger
import scala.build.blooprifle.BloopRifleConfig
import scala.build.options.BuildOptions

case class BspReloadableOptions(
  buildOptions: BuildOptions,
  bloopRifleConfig: BloopRifleConfig,
  logger: Logger,
  verbosity: Int
)

object BspReloadableOptions {
  case class Reference(getReloaded: () => BspReloadableOptions) {
    private val ref: AtomicReference[BspReloadableOptions] = new AtomicReference(getReloaded())

    def get: BspReloadableOptions = ref.get()
    def reload(): Unit = {
      val reloaded = getReloaded()
      if (ref.get() != reloaded) ref.set(reloaded)
    }
  }
}
