package cjdns.cc.dht

import collection.mutable
import concurrent.Promise
import cjdns.cc.DHT

/**
 * User: willzyx
 * Date: 05.07.13 - 21:02
 */
class ServerContext {

  val connections = mutable.LinkedHashMap.empty[I, Connection]

  /* */

  class Query(val rnd: Long = System.currentTimeMillis,
              val promise: Promise[DHT.Packet] = Promise[DHT.Packet](),
              val timestamp: Long = System.currentTimeMillis)

  val queries = mutable.LinkedHashMap.empty[(I, Long), Query]

  def ask(i: I, packet: DHT.Packet)(implicit server: Server) {
    val query = new Query()
    queries.put((i, query.rnd), query)
    server.submit(i, packet.toBuilder.setRnd(query.rnd).build)
  }

  /* */

  def getBucketFullness(l: Int) =
    connections.valuesIterator.
      filter(_.getQuality > 0).
      count(_.distance == l)

  val storage = new Storage

}
