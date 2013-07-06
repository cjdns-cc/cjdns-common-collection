package cjdns.cc.dht

import cjdns.cc.DHT
import concurrent.Promise

/**
 * User: willzyx
 * Date: 05.07.13 - 23:40
 */
class TaskAsk(i: I,
              packet: DHT.Packet,
              promise: Promise[DHT.Packet])(implicit server: Server, context: ServerContext) extends Runnable {
  def run() {
    context.ask(
      i = i,
      packet = packet,
      promise = promise
    )
  }
}
