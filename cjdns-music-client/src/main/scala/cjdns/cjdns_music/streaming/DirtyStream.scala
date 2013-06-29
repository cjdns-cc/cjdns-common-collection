package cjdns.cjdns_music.streaming

import cjdns.cjdns_music.Model
import collection.mutable
import java.util.{TimerTask, Timer}
import scala.concurrent.duration._
import collection.mutable.ListBuffer

/**
 * User: willzyx
 * Date: 29.06.13 - 21:15
 */
object DirtyStream {
  val TIMER_PERIOD = 30.seconds
  val OVERALL_SLOTS = 50000

  private val LOCK = new Object
  private val map = mutable.LinkedHashMap.empty[String, DirtyCollector]

  private def getCollector(key: String) = LOCK.synchronized(map.getOrElseUpdate(key, new DirtyCollector))

  new Timer("dirty_stream_manager", true).schedule(
    new TimerTask {
      def run() {
        implicit val NOW = System.currentTimeMillis
        LOCK.synchronized {
          var list = ListBuffer.empty[DirtyCollector]
          for ((key, collector) <- map; if !collector.isOnline) {
            map -= key
            list += collector
          }
          list
        } foreach (_.purge())
        val collectors = LOCK.synchronized(map.values.toList)
        val weight = collectors.toIterator.map(_.getScore).sum
        if (weight > 0) {
          collectors.foreach(
            collector =>
              collector.setHeapCapacity(
                (collector.getScore * OVERALL_SLOTS / weight).toInt
              )
          )
        }
      }
    },
    TIMER_PERIOD.toMillis, TIMER_PERIOD.toMillis
  )

  def touch(key: String) {
    getCollector(key).touch
  }

  def put(key: String, record: Model.Record) {
    getCollector(key).putRecord(record)
  }

  def putAll(key: String, records: List[Model.Record]) {
    getCollector(key).putRecords(records)
  }

  def getFilter(key: String): Model.FilterDirty = {
    getCollector(key).getFilter
  }


}
