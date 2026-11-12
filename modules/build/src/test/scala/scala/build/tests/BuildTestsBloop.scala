package scala.build.tests

import scala.build.tests.util.BloopServer

class BuildTestsBloop extends BuildTests(server = true) with BloopServer.Suite
