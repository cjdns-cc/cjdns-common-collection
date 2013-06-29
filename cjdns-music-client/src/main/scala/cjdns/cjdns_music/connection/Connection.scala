package cjdns.cjdns_music.connection

import org.jboss.netty.channel.Channel
import cjdns.cjdns_music.Model

/**
 * User: willzyx
 * Date: 29.06.13 - 17:58
 */
abstract class Connection(channel: Channel) {
  def message(msg: Model.TransportPacket)

  def close()
}
