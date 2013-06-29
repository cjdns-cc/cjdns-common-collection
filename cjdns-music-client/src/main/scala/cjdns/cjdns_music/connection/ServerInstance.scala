package cjdns.cjdns_music.connection

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import cjdns.util.Threads
import java.net.InetSocketAddress
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.frame.{LengthFieldPrepender, LengthFieldBasedFrameDecoder}
import org.apache.commons.io.FileUtils
import org.jboss.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufDecoder}
import cjdns.cjdns_music.Model

/**
 * User: willzyx
 * Date: 28.06.13 - 19:10
 */
class ServerInstance(port: Int, handler: Channel => Connection) {
  val WORKERS_COUNT = 2
  val channelFactory =
    new NioServerSocketChannelFactory(
      Threads.newFixedPool(capacity = 1, names = Iterator.continually("server-boss")),
      Threads.newFixedPool(capacity = WORKERS_COUNT, names = Iterator.from(1).map("server-worker-" + _)),
      WORKERS_COUNT
    )

  val server = new ServerBootstrap(channelFactory)
  server.setOption("backlog", 500)
  server.setPipelineFactory(
    new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline =
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

            override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

              connection.message(e.getMessage.asInstanceOf[Model.TransportPacket])
            }

            override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
              this.connection.close()
            }

            override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
              // do nothing
            }
          }
        )
    }
  )
  val channel = server.bind(new InetSocketAddress(port))

  def close() {
    channel.close()
    channelFactory.releaseExternalResources()
  }

}