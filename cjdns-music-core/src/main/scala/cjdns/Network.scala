package cjdns

import java.net.{Inet6Address, NetworkInterface}
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 04.07.13 - 23:55
 */
object Network {
  val LOCAL_ADDRESS: Inet6Address = {
    NetworkInterface.getByName("tun0").getInetAddresses.toList collect {
      case address: Inet6Address => address
    }
  }.head
}
