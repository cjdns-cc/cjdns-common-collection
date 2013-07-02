package cjdns.cjdns_music.connection

import cjdns.cjdns_music.Model.TransportPacket
import java.net.InetSocketAddress
import scala.concurrent.duration._
import cjdns.cjdns_music.Model
import cjdns.cjdns_music.streaming.DirtyPoolShepherd
import scala.collection.JavaConversions._
import cjdns.util.collection.BloomFilterFactory

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
          val pool = DirtyPoolShepherd.getScoringPool(ip)
          watcher.touch()
          pool.acceptScores()

          if (msg.hasPing) {
            if (nextDirtyFlush < NOW) {
              nextDirtyFlush = NOW + CHECK_DIRTY_PERIOD.toMillis
              val items = pool.heap.getItems
              val bloom = BloomFilterFactory.newDefault(items.size)
              items.foreach(item => bloom.add(item.getId))
              channel.write(
                Model.TransportPacket.newBuilder.setDirtyFilter(
                  Model.TransportPacket.DirtyFilter.newBuilder.
                    setBloom(bloom.getMessage).
                    setMusicRecordSlots(pool.heap.getHeapCapacity)
                ).build
              )
            }
          }

          if (msg.hasDirtyData) {
            pool.heap.
              addItems(msg.getDirtyData.getMusicRecordList.toIterable)
          }

        }

        def close() {

        }
      }
    }
  )

}
