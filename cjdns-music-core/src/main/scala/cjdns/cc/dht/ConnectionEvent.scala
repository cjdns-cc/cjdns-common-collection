package cjdns.cc.dht

import concurrent.duration.FiniteDuration

/**
 * User: willzyx
 * Date: 05.07.13 - 2:59
 */
trait ConnectionEvent extends Comparable[ConnectionEvent] {
  def timestamp: Long

  def compareTo(event: ConnectionEvent): Int = if (timestamp < event.timestamp) -1 else 1

  def failure: Boolean
}

object ConnectionEvent {

  case class Reply(timestamp: Long, latency: FiniteDuration) extends ConnectionEvent {
    def failure = false
  }

  case class Timeout(timestamp: Long) extends ConnectionEvent {
    def failure = true
  }

}