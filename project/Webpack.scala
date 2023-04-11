import play.sbt.PlayRunHook
import sbt._

import scala.sys.process._

object Webpack {
  def apply(base: File): PlayRunHook = {
    object WebpackHook extends PlayRunHook {
      var process: Option[Process] = None

      override def afterStarted(): Unit = {
        val command = Seq("npm", "run", "watch")
        val os = sys.props("os.name").toLowerCase
        val makeCmd = os match {
          case x if x contains "windows" => Seq("cmd", "/C") ++ command
          case _ => command
        }
        process = Some(makeCmd.run)
      }

      override def afterStopped(): Unit = {
        process.foreach(_.destroy())
        process = None
      }
    }

    WebpackHook
  }
}
