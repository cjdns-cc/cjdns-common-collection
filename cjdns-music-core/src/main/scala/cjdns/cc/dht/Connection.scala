package cjdns.cc.dht

import java.net.{DatagramPacket, DatagramSocket}
import java.util.{TimerTask, Timer}
import scala.concurrent.duration._
import util.Random
import cjdns.cc.DHT
import org.slf4j.LoggerFactory
import collection.mutable
import concurrent._

/**
 * User: willzyx
 * Date: 05.07.13 - 13:31
 */
class Connection(socket: DatagramSocket, val address: I) {
  val inetAddress = address.toAddress

  private class Query(val promise: Promise[DHT.Packet],
                      val timestamp: Long = System.currentTimeMillis)

  private val QUERIES_LOCK = new Object
  private val queries = mutable.LinkedHashMap.empty[Long, Query]

  private val EVENTS_LOCK = new Object
  private val events = mutable.TreeSet.empty[ConnectionEvent]

  private val pingTask =
    new TimerTask {
      def run() {
        val NOW = System.currentTimeMillis
        val lostQueries =
          QUERIES_LOCK.synchronized {
            val LOW_THRESHOLD = NOW - Connection.TIMEOUT.toMillis
            val keys = {
              queries.toIterator.takeWhile {
                case (key, query) => query.timestamp < LOW_THRESHOLD
              }
            }.map(_._1).toList
            keys.flatMap(queries.remove)
          }
        lostQueries.foreach(_.promise failure (new Connection.TimeoutException))
        EVENTS_LOCK.synchronized {
          lostQueries.toIterator.
            map(_.timestamp).
            map(ConnectionEvent.Timeout.apply).
            foreach(events += _)
          val LOW_THRESHOLD = NOW - Connection.HISTORY_EXPIRE.toMillis
          events.toIterator.
            takeWhile(_.timestamp < LOW_THRESHOLD).
            toList.
            foreach(events -= _)
        }

        send(
          DHT.Packet.newBuilder.setFindNode(
            DHT.FindNode.newBuilder.
              setPrimaryKey(LOCAL_I.toProto)
          ).build
        )

      }
    }
  Connection.TIMER.schedule(
    pingTask,
    Random.nextInt(Connection.PING_PERIOD.toMillis.toInt),
    Connection.PING_PERIOD.toMillis
  )

  def send(packet: DHT.Packet): Future[DHT.Packet] = {
    val rnd = Random.nextLong()
    val p = promise[DHT.Packet]()
    QUERIES_LOCK.synchronized {
      queries.put(rnd, new Query(p))
    }
    val buffer = packet.toBuilder.setRnd(rnd).build.toByteArray
    try {
      socket.send(new DatagramPacket(buffer, buffer.length, inetAddress))
    } catch {
      case e: Exception =>
        p failure e
    }
    p.future
  }

  def receive(packet: DHT.Packet) {
    QUERIES_LOCK.synchronized(
      queries.remove(packet.getRnd)
    ).foreach {
      case query =>
        query.promise success packet
    }
  }

  def stop() {
    pingTask.cancel()
  }

}

object Connection {
  val TIMEOUT = 10.seconds
  val HISTORY_EXPIRE = 30.minutes

  class TimeoutException extends Exception

  val log = LoggerFactory.getLogger("dht.connection")
  val PING_PERIOD = 1.minutes
  val TIMER = new Timer("dht-timer", false)
}