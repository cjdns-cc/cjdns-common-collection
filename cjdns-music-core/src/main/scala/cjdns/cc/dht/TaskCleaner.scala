package cjdns.cc.dht

import scala.concurrent.duration._

/**
 * User: willzyx
 * Date: 06.07.13 - 15:15
 */
class TaskCleaner(implicit server: Server, context: ServerContext) extends Runnable {
  def run() {
    context.connections.valuesIterator.toList.
      groupBy(_.distance).valuesIterator.
      filter(_.size > K).
      map(_.sortBy(_.getQuality).reverse).
      map(_.drop(K)).
      foreach(_.foreach(connection => context.connections.remove(connection.i)))

    for (connection <- context.connections.valuesIterator) {
      connection.clean(3.hours)
    }
  }
}
