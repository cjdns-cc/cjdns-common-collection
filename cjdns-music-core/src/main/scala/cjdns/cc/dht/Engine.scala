package cjdns.cc.dht

import concurrent._
import scala.concurrent.duration._
import collection.mutable
import cjdns.cc.DHT
import java.util.concurrent.Executors
import scala.collection.JavaConversions._
import scala.util.Try
import scala.util.Success
import scala.util.Failure

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

    val answer = promise[List[I]]()

    val queue = mutable.TreeSet.empty[I]
    val processing = mutable.HashSet.empty[I]
    val successPer = mutable.TreeSet[I](LOCAL_I)
    val failurePer = mutable.HashSet.empty[I]

    def process(i: I) {
      queue -= i
      processing += i
      server.ask(i, query).onComplete(pf(i))
    }

    /* */

    def pf(i: I): Try[DHT.Packet] => Unit = {
      reply => {
        reply match {
          case Success(packet) if packet.hasResponse =>
            successPer += i
            for {
              key <- packet.getResponse.getPartitionKeyList
              i <- Try(I.fromProto(key))
              if !processing.contains(i)
            } {
              queue.add(i)
            }
          case Success(packet) =>
            failurePer += i
          case Failure(e) =>
            failurePer += i
        }
        process(queue.firstKey)
      }
    }

    Await.result(server.getConnections, 1.second).foreach(queue += _)
    future {
      queue.toIterator.
        take(CONCURRENCY).
        foreach(process)
    }

    answer.future
  }
}

object Engine {
  implicit val executor: ExecutionContextExecutor = {
    ExecutionContext.fromExecutor(
      Executors.newSingleThreadExecutor()
    )
  }
}