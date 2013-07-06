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

/**
 * User: willzyx
 * Date: 05.07.13 - 20:11
 */
class Server(val port: Int) {
  implicit val server = Server.this
  implicit val context = new ServerContext

  private val socket = new DatagramSocket
  socket.bind(new InetSocketAddress(Network.LOCAL_ADDRESS, port))

  private val timer = new Timer("dht-timer", false)
  private val worker = Executors.newSingleThreadExecutor

  def submit(i: I, packet: DHT.Packet) {
    val buffer = packet.toByteArray
    socket.send(
      new DatagramPacket(
        buffer,
        buffer.length,
        i.toAddress
      )
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
                      worker.execute(new TaskPacketReceived(I(address), packet))
                  }
              }
            case Failure(e) =>

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
