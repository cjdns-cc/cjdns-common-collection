package cjdns.cc

import cjdns.Network
import scala.concurrent.duration._

/**
 * User: willzyx
 * Date: 05.07.13 - 0:06
 */
package object dht {

  val PORT = 39015
  val K = 20

  val LOCAL_I = I(Network.LOCAL_ADDRESS)
  val REQUEST_TIMEOUT = 10.seconds

}
