package cjdns.cc.dht

import java.net._
import cjdns.Network
import java.util.{TimerTask, Timer}
import java.util.concurrent.Executors
import util.{Random, Try, Success, Failure}
import cjdns.cc.DHT
import java.io.ByteArrayInputStream
import concurrent.duration._
import concurrent.{Promise, Future}
import org.slf4j.LoggerFactory

/**
 * User: willzyx
 * Date: 05.07.13 - 20:11
 */
class Server(val port: Int = PORT) {
  val log = LoggerFactory.getLogger("dht.server")

  implicit val server = Server.this
  implicit val context = new ServerContext

  private val socket = new DatagramSocket(new InetSocketAddress(Network.LOCAL_ADDRESS, port))

  private val timer = new Timer("dht-timer", false)
  private val worker = Executors.newSingleThreadExecutor

  def submit(i: I, packet: DHT.Packet) {
    val address = i.toAddress
    log.debug("submit UDP packet to {}\n{}", address, packet)
    val buffer = packet.toByteArray
    socket.send(
      new DatagramPacket(
        buffer,
        buffer.length,
        new InetSocketAddress(address, port)
      )
    )
  }

  def lookup(i: I) {
    submit(
      i,
      DHT.Packet.newBuilder.setFind(
        DHT.Find.newBuilder.
          setPartitionKey(LOCAL_I.toProto)
      ).build
    )
  }

  def ask(i: I, packet: DHT.Packet): Future[DHT.Packet] = {
    val promise = Promise[DHT.Packet]()
    worker.execute(new TaskAsk(i = i, packet = packet, promise = promise))
    promise.future
  }

  def getConnections: Future[List[I]] = {
    val promise = Promise[List[I]]()
    worker.execute(new TaskGetConnections(promise))
    promise.future
  }

  private val thread =
    new Thread("dht-receiver") {
      override def run() {
        val buffer = new Array[Byte](512)
        while (true) {
          val datagram = new DatagramPacket(buffer, buffer.length)
          Try(socket.receive(datagram)) match {
            case Success(_) =>
              Try(
                DHT.Packet.parseFrom(
                  new ByteArrayInputStream(
                    datagram.getData,
                    datagram.getOffset,
                    datagram.getLength
                  )
                )
              ) foreach {
                case packet =>
                  Option(datagram.getAddress) collect {
                    case address: Inet6Address =>
                      val i = I(address)
                      log.debug("received UDP packet from {}\n{}", address, packet)
                      if (i != LOCAL_I) {
                        worker.execute(new TaskPacketReceived(i, packet))
                      }
                  }
              }
            case Failure(e) =>
              log.error("exception", e)
          }
        }
      }
    }
  thread.start()

  def scheduleTick(i: I, period: FiniteDuration) {
    timer.schedule(
      new TimerTask {
        def run() {
          val task = this
          worker.execute(
            new TaskTick(i) {
              def cancel() {
                task.cancel()
              }
            }
          )
        }
      },
      Random.nextInt(period.toMillis.toInt),
      period.toMillis
    )
  }

  private def scheduleAction(action: => Runnable, period: FiniteDuration) {
    timer.schedule(
      new TimerTask {
        def run() {
          worker.execute(action)
        }
      },
      Random.nextInt(period.toMillis.toInt),
      period.toMillis
    )
  }

  scheduleAction(new TaskTimeoutSetter, 1.second)
  scheduleAction(new TaskCleaner, 1.minute)

  def stop() {
    thread.interrupt()
    timer.cancel()
    worker.shutdown()
    socket.close()
  }
}
