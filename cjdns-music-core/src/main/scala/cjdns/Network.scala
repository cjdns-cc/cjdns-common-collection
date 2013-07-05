package cjdns

import java.net.NetworkInterface
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 04.07.13 - 23:55
 */
object Network {
  val LOCAL_ADDRESS =
    NetworkInterface.getByName("tun0").getInetAddresses.toList.head
}
