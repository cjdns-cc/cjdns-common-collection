package cjdns.cc.dht

import cjdns.cc.DHT

/**
 * User: willzyx
 * Date: 05.07.13 - 21:17
 */
abstract class TaskTick(i: I)(implicit server: Server, context: ServerContext) extends Runnable {
  final def run() {
    if (context.connections contains i) {
      context.ask(
        i,
        DHT.Packet.newBuilder.setFind(
          DHT.Find.newBuilder.
            setPartitionKey(LOCAL_I.toProto)
        ).build
      )
    } else {
      cancel()
    }
  }

  def cancel()
}
