package cjdns.cjdns_music.streaming

import java.util
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 01.07.13 - 19:40
 */
class DisplacingPool[T](private var capacity: Int, trigger: DisplacingPool.Trigger[T] = DisplacingPool.EMPTY_TRIGGER) {
  private val LOCK = new Object
  private val tree = new util.TreeSet[T]

  private def pollExceed: Set[T] = {
    if (tree.size > capacity) {
      tree.descendingIterator.toIterator.
        take(tree.size - capacity).toSet.
        filter(tree.remove)
    } else Set.empty
  }

  def getHeapCapacity: Int = LOCK.synchronized(capacity)

  def setHeapCapacity(size: Int) {
    LOCK.synchronized {
      this.capacity = size
      trigger.leave(pollExceed)
    }
  }

  def getHeapSize: Int = LOCK.synchronized(tree.size)

  def filled: Boolean = LOCK.synchronized(tree.size >= capacity)

  def getItems: List[T] =
    LOCK.synchronized(tree.toList)

  def getTopItems(count: Int): List[T] =
    LOCK.synchronized(tree.toIterator.take(count).toList)

  def addItem(item: T) {
    LOCK.synchronized {
      if (tree.add(item)) {
        val exceed = pollExceed
        if (!exceed.contains(item)) {
          trigger.enter(item)
          trigger.leave(exceed)
        }
      }
    }
  }

  def addItems(items: Iterable[T]) {
    LOCK.synchronized {
      trigger.enter(items.filter(tree.add))
      trigger.leave(pollExceed)
    }
  }

  def removeItems(items: Iterable[T]) {
    LOCK.synchronized {
      trigger.leave(items.filter(tree.remove))
    }
  }

  def removeItem(item: T) {
    LOCK.synchronized {
      if (tree.remove(item)) {
        trigger.leave(item)
      }
    }
  }

  def purge() {
    LOCK.synchronized {
      trigger.leave(tree)
      tree.clear()
    }
  }

}

object DisplacingPool {

  trait Trigger[T] {
    def enter(item: T)

    def enter(items: Iterable[T])

    def leave(item: T)

    def leave(items: Iterable[T])
  }

  def EMPTY_TRIGGER[T] =
    new Trigger[T] {
      def enter(item: T) {}

      def enter(items: Iterable[T]) {}

      def leave(item: T) {}

      def leave(items: Iterable[T]) {}
    }

}