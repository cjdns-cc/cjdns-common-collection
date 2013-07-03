package cjdns.cjdns_music.connection

import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import cjdns.util.Threads
import org.jboss.netty.bootstrap.ClientBootstrap
import org.apache.commons.io.FileUtils
import org.jboss.netty.handler.codec.frame.{LengthFieldPrepender, LengthFieldBasedFrameDecoder}
import org.jboss.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufDecoder}
import org.jboss.netty.channel._
import java.util.{Timer, TimerTask}
import cjdns.cjdns_music.Model
import collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import util.Try

/**
 * User: willzyx
 * Date: 29.06.13 - 2:03
 */
class ClientInstance(handler: Channel => Connection) {
  val log = LoggerFactory.getLogger("outgoing-connection")

  val WORKERS_COUNT = 2

  val channelFactory =
    new NioClientSocketChannelFactory(
      Threads.newFixedPool(capacity = 1, names = Iterator.continually("client-boss")),
      Threads.newFixedPool(
        capacity = WORKERS_COUNT,
        names = Iterator.from(1).map("client-worker-" + _)
      ),
      WORKERS_COUNT
    )

  private class WatchTask(val address: InetSocketAddress) extends TimerTask {
    var channel: Channel = null

    def run() {
      if (channel == null || !channel.isConnected) {
        channel = null
        connect(address, this)
      } else {
        channel.write(ClientInstance.PING_PACKET)
      }
    }

    def setChannel(channel: Channel) {
      this.channel = channel
    }
  }

  private def connect(address: InetSocketAddress, watcher: WatchTask) {
    val client = new ClientBootstrap(channelFactory)
    client.setOption("tcpNoDelay", false)
    client.setOption("receiveBufferSize", FileUtils.ONE_MB * 4)
    client.setOption("connectTimeoutMillis", TIMEOUT.toMillis)
    client.setPipeline(
      Channels.pipeline(
        new LengthFieldBasedFrameDecoder(FileUtils.ONE_MB.toInt * 4, 0, 4, 0, 4),
        new ProtobufDecoder(Model.TransportPacket.getDefaultInstance),
        new LengthFieldPrepender(4),
        new ProtobufEncoder,
        new SimpleChannelUpstreamHandler {
          var connection: Connection = null

          override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
            this.connection = handler(ctx.getChannel)
          }

          override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
            this.connection.close()
          }

          override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
            connection.message(e.getMessage.asInstanceOf[Model.TransportPacket])
          }

          override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
            log.error("failure", e.getCause)
          }
        }
      )
    )
    client.connect(address).addListener(
      new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          if (future.isSuccess) {
            watcher.setChannel(future.getChannel)
          } else {
            if (getChannel.isDefined) {
              log.warn("problem while connecting to \"%s\"" format address)
            } else {
              log.error("problem while connecting to \"%s\"" format address)
            }
          }
        }
      }
    )
  }

  private val watchTimer = new Timer("server-pinging")
  private var watchers = ListBuffer.empty[WatchTask]

  def add(address: InetSocketAddress) {
    val watcher = new WatchTask(address)
    watchers += watcher
    watchTimer.schedule(watcher, 0, (TIMEOUT * 2 / 5).toMillis)
  }

  def getChannel = watchers.find(_.channel.isConnected).map(_.channel)

  def close() {
    watchTimer.cancel()
    for {
      watcher <- watchers
      channel <- Option(watcher.channel)
    } Try {
      channel.close().sync()
    }
    channelFactory.releaseExternalResources()
  }

}

object ClientInstance {
  val PING_PACKET =
    Model.TransportPacket.newBuilder.
      setPing(Model.TransportPacket.Ping.getDefaultInstance).
      build
}