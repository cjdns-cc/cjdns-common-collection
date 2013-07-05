package cjdns.cc.dht

import concurrent.duration.FiniteDuration

/**
 * User: willzyx
 * Date: 05.07.13 - 2:59
 */
trait ConnectionEvent extends Comparable[ConnectionEvent] {
  def timestamp: Long

  def compareTo(event: ConnectionEvent): Int = if (timestamp < event.timestamp) -1 else 1
}

object ConnectionEvent {

  case class PingPong(timestamp: Long, latency: FiniteDuration) extends ConnectionEvent

  case class Timeout(timestamp: Long) extends ConnectionEvent

}