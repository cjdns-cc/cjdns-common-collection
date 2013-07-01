package cjdns.cjdns_music.connection

import cjdns.cjdns_music.Model.TransportPacket
import java.net.InetSocketAddress
import scala.concurrent.duration._
import cjdns.cjdns_music.Model
import cjdns.cjdns_music.streaming.DirtyCollectorManager
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 29.06.13 - 18:33
 */
object FlowBottom {
  val CHECK_DIRTY_PERIOD = 3.minutes
  val config = cjdns.cjdns_music.properties.getConfig("server")

  new ServerInstance(
    cjdns.cjdns_music.properties.getInt("port"),
    channel => {
      val watcher = Watcher.watch(channel)
      val ip = channel.getRemoteAddress.asInstanceOf[InetSocketAddress].getAddress.getHostAddress
      new Connection(channel) {
        var nextDirtyFlush = System.currentTimeMillis

        def message(msg: TransportPacket) {
          val NOW = System.currentTimeMillis
          watcher.touch()
          if (msg.hasPing) {
            if (nextDirtyFlush < NOW) {
              nextDirtyFlush = NOW + CHECK_DIRTY_PERIOD.toMillis
              channel.write(
                Model.TransportPacket.newBuilder.
                  setDirtyFilter(DirtyCollectorManager.getFilter(ip)).
                  build
              )
            }
          }
          if (msg.hasDirtyData) {
            msg.getDirtyData.getRecordList.
              foreach(DirtyCollectorManager.put(ip, _))
          }
        }

        def close() {

        }
      }
    }
  )

}
