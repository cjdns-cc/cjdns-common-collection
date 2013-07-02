package cjdns.cjdns_music.connection

import cjdns.cjdns_music.Model.TransportPacket
import scala.collection.JavaConversions._
import java.net.InetSocketAddress
import cjdns.cjdns_music.streaming.DirtyPoolShepherd
import cjdns.util.collection.BloomFilter

/**
 * User: willzyx
 * Date: 29.06.13 - 18:32
 */
object FlowTop {
  private val config = cjdns.cjdns_music.properties.getConfig("connect-to")

  val REF = new ClientInstance(
    channel => {
      val watcher = Watcher.watch(channel)
      new Connection(channel) {
        def message(msg: TransportPacket) {
          watcher.touch()
          if (msg.hasDirtyFilter) {
            val dirtyFilter = msg.getDirtyFilter
            val pools = DirtyPoolShepherd.LOCAL_POOL :: DirtyPoolShepherd.getPools
            val counts = {
              val count = pools.map(_.getHeapSize)
              val sum = count.sum
              if (sum == 0) count
              else count.map(i => i * dirtyFilter.getMusicRecordSlots / sum)
            }
            val bloom = BloomFilter.restore(dirtyFilter.getBloom)
            val builder = TransportPacket.DirtyData.newBuilder
            def musicRecords =
              (pools zip counts) map {
                case (pool, count) =>
                  val list = pool.getTopItems(count)
                  (Iterator.from(list.size, -1).map(_.toDouble / list.size) zip list.toIterator) map {
                    case (weight, item) => item.toBuilder.setWeight(weight).build
                  }
              } reduce (_ ++ _) filterNot (item => bloom.contains(item.getId))
            musicRecords.foreach(builder.addMusicRecord(_))
            channel.write(
              TransportPacket.newBuilder.
                setDirtyData(builder).
                build
            )
          }
        }

        def close() {

        }
      }
    }
  )

  config.getConfigList("address-list").foreach(
    address =>
      REF.add(
        new InetSocketAddress(
          address.getString("host"),
          address.getInt("port")
        )
      )
  )

}
