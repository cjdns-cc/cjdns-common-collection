package cjdns.cc.dht

import concurrent._
import collection.mutable
import cjdns.cc.DHT
import java.util.concurrent.Executors
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * User: willzyx
 * Date: 05.07.13 - 23:02
 */
class Engine(server: Server) {

  import Engine.executor

  val CONCURRENCY = 5

  def findPer(target: I): Future[List[I]] = {
    implicit object Ord extends Ordering[I] {
      def compare(x: I, y: I): Int = {
        val l1 = x ? target
        val l2 = y ? target
        if (l1 < l2) -1
        else if (l1 > l2) 1
        else x compareTo y
      }
    }

    val query =
      DHT.Packet.newBuilder.setFind(
        DHT.Find.newBuilder.
          setPartitionKey(target.toProto)
      ).build

    val queue = mutable.TreeSet.empty[I]
    val processing = mutable.HashSet.empty[I]
    val successPer = mutable.TreeSet[I](LOCAL_I)

    def pollFirst: Option[I] = {
      if (!queue.isEmpty)
        Option(queue.firstKey)
      else
        Option.empty
    }.filter(queue.remove)

    def process(i: I): Future[I] = {
      processing += i
      server.ask(i, query) collect {
        case packet if packet.hasResponse =>
          successPer += i
          for {
            key <- packet.getResponse.getPartitionKeyList
            i <- Try(I.fromProto(key))
            if !processing.contains(i)
          } {
            queue.add(i)
          }
      } recover {
        case e => Unit
      } map {
        case _ => pollFirst
      } collect {
        case Some(v) => v
      } flatMap {
        case ii => process(ii)
      }
    }

    /* */
    server.getConnections.flatMap(
      list => {
        list.foreach(queue += _)
        List.fill(CONCURRENCY)(pollFirst).
          filter(_.isDefined).
          map(_.get).
          map(i => process(i)).
          foldLeft(Future.successful[Any](Unit))(_ zip _).
          map(_ => successPer.toIterator.take(K).toList)
      }
    )
  }
}

object Engine {
  implicit val executor: ExecutionContextExecutor = {
    ExecutionContext.fromExecutor(
      Executors.newSingleThreadExecutor
    )
  }
}