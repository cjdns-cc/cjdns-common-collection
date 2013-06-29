package cjdns.util

import java.util.concurrent.{ThreadFactory, Executors}

/**
 * User: willzyx
 * Date: 28.06.13 - 19:19
 */
object Threads {
  def newFixedPool(capacity: Int, names: Iterator[String]) = {
    Executors.newFixedThreadPool(
      capacity,
      new ThreadFactory {
        val LOCK = new Object

        def newThread(r: Runnable): Thread = {
          val thread = new Thread(r, LOCK.synchronized(names.next()))
          thread.setDaemon(true)
          thread
        }
      }
    )
  }
}
