package cjdns.cjdns_music.streaming

import cjdns.cjdns_music.Model
import collection.mutable
import java.util.{Comparator, TimerTask, Timer}
import scala.concurrent.duration._
import cjdns.cjdns_music.Model.MusicRecord
import java.util.concurrent.atomic.AtomicLong

/**
 * User: willzyx
 *
 * Date: 29.06.13 - 21:15
 */
object DirtyPoolShepherd {
  val MIN_SCORE_BOUND = 5.minutes.toMillis
  val OVERALL_SLOTS = 50000

  private val RECORD_COMPARATOR =
    new Comparator[Model.MusicRecord] {
      def compare(r1: MusicRecord, r2: MusicRecord): Int =
        if (r1.getId == r2.getId) 0
        else if (r1.getWeight < r2.getWeight) -1 else 1
    }

  val LOCAL_POOL =
    new DisplacingPool[Model.MusicRecord](
      capacity = OVERALL_SLOTS,
      comparator = RECORD_COMPARATOR
    )

  class ScoringPool {
    val heap =
      new DisplacingPool[Model.MusicRecord](
        capacity = 100,
        comparator = RECORD_COMPARATOR
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

  def getScoringPool(key: String) = REMOTE_POOLS_LOCK.synchronized(REMOTE_POOLS.getOrElseUpdate(key, new ScoringPool))

  def getScoringPools = REMOTE_POOLS_LOCK.synchronized(REMOTE_POOLS.valuesIterator.toList)

  def getPools = REMOTE_POOLS_LOCK.synchronized(REMOTE_POOLS.valuesIterator.map(_.heap).toList)

  private val timer = new Timer("dirty_stream_manager", true)
  timer.schedule(
    new TimerTask {
      def run() {
        REMOTE_POOLS_LOCK.synchronized {
          val keys = {
            REMOTE_POOLS.toIterator collect {
              case (key, pool) if !pool.isOnline => key
            }
          }.toList
          keys.foreach(REMOTE_POOLS -= _)
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
    60.seconds.toMillis, 60.seconds.toMillis
  )

}
