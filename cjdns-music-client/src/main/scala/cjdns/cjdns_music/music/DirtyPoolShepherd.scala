package cjdns.cjdns_music.music

import cjdns.cjdns_music.AtomicItem
import collection.mutable
import java.util.{TimerTask, Timer}
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicLong
import cjdns.cjdns_music.streaming.DisplacingPool.Trigger
import cjdns.cjdns_music.AtomicItem.WMusicRecord
import cjdns.cjdns_music.streaming.DisplacingPool

/**
 * User: willzyx
 *
 * Date: 29.06.13 - 21:15
 */
object DirtyPoolShepherd {
  val MIN_SCORE_BOUND = 5.minutes.toMillis
  val OVERALL_SLOTS = 50000

  val LOCAL_POOL = new DisplacingPool[AtomicItem.WMusicRecord](capacity = OVERALL_SLOTS)

  class ScoringPool(key: String) {
    val heap =
      new DisplacingPool[AtomicItem.WMusicRecord](
        capacity = 100,
        trigger =
          new Trigger[WMusicRecord] {
            implicit val partition: String = key
            implicit val writer = DirtyIndex.DIRTY_WRITER

            def enter(item: WMusicRecord) {
              index.Tools.writeRecord(item.base)
            }

            def leave(item: WMusicRecord) {
              index.Tools.removeRecord(item.base)
            }
          }
      )
    val timestamp = new AtomicLong(System.currentTimeMillis)
    val scores = new AtomicLong(MIN_SCORE_BOUND)

    def acceptScores() {
      val NOW = System.currentTimeMillis
      val delta = NOW - timestamp.getAndSet(NOW)
      if (delta < 1.minute.toMillis) {
        scores.addAndGet(delta)
      } else {
        scores.addAndGet(-delta * 4)
      }
    }

    def isOnline = getScore > 0

    def getScore = {
      val NOW = System.currentTimeMillis
      val delta = NOW - timestamp.get
      if (delta < 1.minute.toMillis)
        scores.get + delta
      else
        scores.get - delta * 4
    }
  }

  private val REMOTE_POOLS_LOCK = new Object
  private val REMOTE_POOLS = mutable.LinkedHashMap.empty[String, ScoringPool]

  def getScoringPool(key: String) =
    REMOTE_POOLS_LOCK.synchronized(
      REMOTE_POOLS.getOrElseUpdate(
        key,
        new ScoringPool(key)
      )
    )

  def getScoringPools = REMOTE_POOLS_LOCK.synchronized(REMOTE_POOLS.valuesIterator.toList)

  def getPools = REMOTE_POOLS_LOCK.synchronized(REMOTE_POOLS.valuesIterator.map(_.heap).toList)

  def initialize() {
    val timer = new Timer("dirty_stream_manager", true)
    timer.schedule(
      new TimerTask {
        def run() {
          REMOTE_POOLS_LOCK.synchronized {
            val keys = {
              REMOTE_POOLS.toIterator collect {
                case (key, pool) if !pool.isOnline => key
              }
            }.toList
            keys.foreach(key => REMOTE_POOLS.remove(key).foreach(_.heap.purge()))
          }
        }
      },
      30.seconds.toMillis, 60.seconds.toMillis
    )
    timer.schedule(
      new TimerTask {
        var k: Double = 1

        def run() {
          val pools = REMOTE_POOLS_LOCK.synchronized(REMOTE_POOLS.valuesIterator.toList).filter(_.getScore > 0)
          if (!pools.isEmpty) {
            val used = pools.toIterator.map(_.heap.getHeapSize).sum
            val overall = math.max(OVERALL_SLOTS - LOCAL_POOL.getHeapSize, 1)
            if (pools.exists(_.heap.filled) && used > 0) {
              this.k = this.k * overall / used
            }
            pools.foreach {
              case pool =>
                pool.heap.setHeapCapacity(
                  math.min(
                    math.max(pool.heap.getHeapSize * 2, 100),
                    (pool.getScore * this.k).toInt
                  )
                )
            }
          }
        }
      },
      15.seconds.toMillis, 29.seconds.toMillis
    )
  }

}
