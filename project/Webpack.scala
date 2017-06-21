import java.net.InetSocketAddress
import play.sbt.PlayRunHook
import sbt._

object Webpack {
  def apply(base: File): PlayRunHook = {
    object WebpackHook extends PlayRunHook {
      var process: Option[Process] = None

      override def afterStarted(addr: InetSocketAddress) = {
        val command = Seq("npm", "run", "watch")
        val os = sys.props("os.name").toLowerCase
        val makeCmd = os match {
          case x if x contains "windows" => Seq("cmd", "/C") ++ command
          case _ => command
        }
        process = Option(makeCmd.run())
      }

      override def afterStopped() = {
        process.foreach(_.destroy())
        process = None
      }
    }

    WebpackHook
  }
}
