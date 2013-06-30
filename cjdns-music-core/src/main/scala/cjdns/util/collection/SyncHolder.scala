package cjdns.util.collection

import annotation.tailrec
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import cjdns.util.concurrent.ReadWriteLockWrapper

/**
 * User: willzyx
 * Date: 01.07.13 - 0:28
 */
final class SyncHolder[T](value: T) {

  class Item(val value: T) {
    val rwLock = new ReadWriteLockWrapper(new ReentrantReadWriteLock)
    var released = false

    def use[R](handler: T => R): Option[R] = {
      rwLock.tryRead(
        if (!released) {
          Some(handler(value))
        } else {
          None
        }
      ).getOrElse(None)
    }

    def release() {
      rwLock.write(released = true)
    }
  }

  private val ref = new AtomicReference[Item](new Item(value))

  @tailrec
  def use[R](handler: T => R): R =
    ref.get.use(handler) match {
      case Some(r) => r
      case None => use(handler)
    }

  def getAndSet(value: T): T = {
    val item = ref.getAndSet(new Item(value))
    item.release()
    item.value
  }

}