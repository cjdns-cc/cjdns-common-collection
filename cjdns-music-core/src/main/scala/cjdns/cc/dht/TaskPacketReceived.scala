package cjdns.cc.dht

import cjdns.cc.DHT
import scala.collection.JavaConversions._
import util.Try
import scala.concurrent.duration._

/**
 * User: willzyx
 * Date: 05.07.13 - 20:38
 */
class TaskPacketReceived(i: I, packet: DHT.Packet)(implicit server: Server, context: ServerContext) extends Runnable {

  def run() {
    val RND = packet.getRnd

    /* */
    if (packet.hasResponse) {
      context.queries.remove(i -> packet.getRnd) foreach {
        case query =>
          query.promise success packet
      }
      val msg = packet.getResponse
      for {
        partitionKey <- msg.getPartitionKeyList
        i <- Try(I.fromProto(partitionKey))
        if i != LOCAL_I
        if !(context.connections contains i)
        if context.getBucketFullness(i ? LOCAL_I) < K
      } {
        server.submit(
          i,
          DHT.Packet.newBuilder.setFind(
            DHT.Find.newBuilder.
              setPartitionKey(i.toProto)
          ).build
        )
      }
    }

    /* */
    if (packet.hasFind) {
      val msg = packet.getFind
      val i = I.fromProto(msg.getPartitionKey)
      val builder = DHT.Response.newBuilder
      context.connections.valuesIterator.
        filter(_.getQuality > 0).toList.
        sortBy(_.i ? i).
        take(K).toIterator.
        map(_.i.toProto).
        foreach(builder.addPartitionKey(_))
      if (msg.hasPrimaryKey) {
        context.storage.get(
          partition = i,
          key = I.fromProto(msg.getPartitionKey)
        ).foreach(i => builder.addValue(i.toProto))
      }
      server.submit(
        i,
        DHT.Packet.newBuilder.
          setRnd(RND).
          setResponse(builder).
          build
      )
    }

    /* */
    if (packet.hasCheckIn) {
      val msg = packet.getCheckIn
      context.storage.put(
        partition = I.fromProto(msg.getPartitionKey),
        key = I.fromProto(msg.getPrimaryKey),
        value = i
      )
    }

    /* */
    context.getBucketFullness(i ? LOCAL_I) < K
    context.connections.getOrElseUpdate(i, new Connection(i))
    server.scheduleTick(i, 1.minute)
  }

}
