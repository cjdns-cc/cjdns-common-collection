package cjdns.cc.dht

import java.util.concurrent.TimeoutException

/**
 * User: willzyx
 * Date: 06.07.13 - 2:07
 */
class TaskTimeoutSetter(implicit context: ServerContext) extends Runnable {
  def run() {
    val NOW = System.currentTimeMillis
    val THRESHOLD = NOW - REQUEST_TIMEOUT.toMillis
    for ((key@(i, _), query) <- context.queries; if query.timestamp < THRESHOLD) {
      context.queries.remove(key)
      context.connections.get(i).
        foreach(_.put(ConnectionEvent.Timeout(query.timestamp)))
      query.promise failure (new TimeoutException)
    }
  }
}

