package cjdns.cc.dht

import concurrent.Promise

/**
 * User: willzyx
 * Date: 05.07.13 - 23:55
 */
class TaskGetConnections(promise: Promise[List[I]])(implicit context: ServerContext) extends Runnable {
  def run() {
    promise success context.connections.keysIterator.toList
  }
}
