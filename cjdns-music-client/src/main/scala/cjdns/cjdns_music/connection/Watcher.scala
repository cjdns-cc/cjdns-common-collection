package cjdns.cjdns_music.connection

import org.jboss.netty.channel.Channel
import java.util.{TimerTask, Timer}

/**
 * User: willzyx
 * Date: 29.06.13 - 0:13
 */
trait Watcher {
  def touch()
}

object Watcher {
  val timer = new Timer("connection_watcher", true)

  def watch(channel: Channel) = {
    @volatile var expireAt = System.currentTimeMillis + TIMEOUT.toMillis
    timer.schedule(
      new TimerTask {
        def run() {
          if (!channel.isConnected) {
            this.cancel()
          } else if (expireAt < System.currentTimeMillis) {
            channel.close()
            this.cancel()
          }
        }
      },
      TIMEOUT.toMillis, TIMEOUT.toMillis / 3
    )
    new Watcher {
      def touch() {
        expireAt = System.currentTimeMillis + TIMEOUT.toMillis
      }
    }
  }

}