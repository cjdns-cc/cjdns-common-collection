package cjdns.cjdns_music.streaming

import java.util.{Comparator, PriorityQueue}
import cjdns.cjdns_music.Model
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import cjdns.util.collection.BloomFilter
import cjdns.util.number
import util.Random
import org.apache.commons.io.FileUtils
import com.google.protobuf.ByteString

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
    synchronized {
      this.timestampScores = getScore
      this.timestamp = NOW
    }
  }

  def getScore(implicit NOW: Long = System.currentTimeMillis) = {
    synchronized {
      math.min({
        val delta = NOW - timestamp
        if (delta < 1.minute.toMillis) {
          this.timestampScores + delta
        } else {
          this.timestampScores - delta * 4
        }
      }, 0)
    }
  }

  def isOnline = getScore > 0

  def setHeapCapacity(N: Int) {
    synchronized {
      this.heapCapacity = N
      while (heap.size > N) {
        heap.poll()
      }
    }
  }

  def getHeapCapacity = synchronized(heapCapacity)

  def getHeapSize = synchronized(heap.size)

  def putRecord(record: Model.Record) {
    synchronized {
      heap.add(record)
      while (heap.size > heapCapacity) {
        heap.poll()
      }
    }
  }

  def putRecords(records: List[Model.Record]) {
    synchronized {
      records.foreach(heap.add)
      while (heap.size > heapCapacity) {
        heap.poll()
      }
    }
  }

  def getRecords = synchronized(heap.toList)

  def getFilter = {
    val builder = Model.FilterDirty.newBuilder
    val bloom =
      new BloomFilter[String](
        number.primes.drop(Random.nextInt(64) + 16).take(2).toArray,
        64 * FileUtils.ONE_KB * 8
      ) {
        protected def hash(obj: String) = obj.hashCode
      }
    synchronized {
      heap.foreach(record => bloom.add(record.getHash.toStringUtf8))
      builder.setFreeSlots(heapCapacity - heap.size)
    }
    builder.setBloom {
      bloom.getVector.foldLeft(
        Model.Bloom.newBuilder.
          setBitset(ByteString.copyFrom(bloom.getByteArray))
      )(_.addFactor(_))
    }
    builder.build
  }

  def purge() {

  }

}

object DirtyCollector {
  val MIN_SCORE_BOUND = 5.minutes.toMillis
}