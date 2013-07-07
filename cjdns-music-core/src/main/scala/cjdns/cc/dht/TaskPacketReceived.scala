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

  def send(packet: DHT.Packet) {
    server.submit(i, packet)
  }

  def run() {
    val NOW = System.currentTimeMillis
    val RND = packet.getRnd

    /* */
    if (packet.hasResponse) {
      context.queries.remove(i -> packet.getRnd) foreach {
        case query =>
          query.promise success packet
          context.connections.get(i).foreach {
            case connection =>
              connection.put(
                ConnectionEvent.Reply(
                  timestamp = query.timestamp,
                  latency = (NOW - query.timestamp).millis
                )
              )
          }
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
      (LOCAL_I :: context.connections.valuesIterator.filter(_.getQuality > 0).map(_.i).toList).
        sortBy(_ ? i).
        take(K).toIterator.
        map(_.toProto).
        foreach(builder.addPartitionKey(_))
      if (msg.hasPrimaryKey) {
        context.storage.get(
          partition = i,
          key = I.fromProto(msg.getPartitionKey)
        ).foreach(i => builder.addValue(i.toProto))
      }
      send(
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
    context.connections.getOrElseUpdate(i, {
      server.scheduleTick(i, 1.minute)
      new Connection(i)
    })
  }

}
