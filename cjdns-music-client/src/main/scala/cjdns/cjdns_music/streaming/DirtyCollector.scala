package cjdns.cjdns_music.streaming

import java.util.{Comparator, PriorityQueue}
import cjdns.cjdns_music.Model
import scala.concurrent.duration._
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 29.06.13 - 21:54
 */
class DirtyCollector {
  private var heapCapacity: Int = 128
  private val heap = new PriorityQueue[Model.Record](
    heapCapacity,
    new Comparator[Model.Record] {
      def compare(r1: Model.Record, r2: Model.Record) =
        if (r1.getWeight < r2.getWeight) -1 else 1
    }
  )
  private var timestamp: Long = System.currentTimeMillis
  private var timestampScores: Long = DirtyCollector.MIN_SCORE_BOUND

  def touch(implicit NOW: Long = System.currentTimeMillis) {
    this.timestampScores = getScore
    this.timestamp = NOW
  }

  def getScore(implicit NOW: Long = System.currentTimeMillis) = {
    math.min({
      val delta = NOW - timestamp
      if (delta < 1.minute.toMillis) {
        this.timestampScores + delta
      } else {
        this.timestampScores - delta * 4
      }
    }, 0)
  }

  def getOnline = getScore > 0

  def setHeapCapacity(N: Int) {
    this.heapCapacity = N
    while (heap.size > N) {
      heap.poll()
    }
  }

  def getHeapCapacity = heapCapacity

  def addRecord(record: Model.Record) {
    heap.add(record)
    while (heap.size > heapCapacity) {
      heap.poll()
    }
  }

  def getRecords = heap.toIterator

}

object DirtyCollector {
  val MIN_SCORE_BOUND = 5.minutes.toMillis
}