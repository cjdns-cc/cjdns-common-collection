package cjdns.cc.dht

/**
 * User: willzyx
 * Date: 05.07.13 - 21:17
 */
abstract class TaskTick(i: I)(implicit server: Server, context: ServerContext) extends Runnable {
  final def run() {

  }

  def cancel()
}
