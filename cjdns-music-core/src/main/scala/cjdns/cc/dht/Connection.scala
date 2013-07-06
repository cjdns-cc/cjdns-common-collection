package cjdns.cc.dht

import java.util
import scala.collection.JavaConversions._
import concurrent.duration.FiniteDuration

/**
 * User: willzyx
 * Date: 05.07.13 - 13:31
 */
class Connection(val i: I) {
  val distance = i ? LOCAL_I

  private val events = new util.TreeSet[ConnectionEvent]

  def put(event: ConnectionEvent) {
    events.add(event)
  }

  def clean(period: FiniteDuration) {

  }

  def getQuality: Double = {
    val FAILURE_THRESHOLD = 5
    if (events.descendingIterator.toIterator.take(FAILURE_THRESHOLD).count(_.failure) < FAILURE_THRESHOLD) {
      val times = {
        events.toIterator collect {
          case event: ConnectionEvent.Reply =>
            event.latency.toMillis
        }
      }.toList
      if (times.isEmpty) 0D
      else times.size.toDouble / times.sum
    } else -1D
  }
}
