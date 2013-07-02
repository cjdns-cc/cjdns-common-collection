package cjdns.cjdns_music.streaming

import java.util
import util.Comparator
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 01.07.13 - 19:40
 */
class DisplacingPool[T](private var capacity: Int,
                        comparator: Comparator[T]) {
  private val LOCK = new Object
  private val tree = new util.TreeSet[T](comparator)

  private def removeExceed() {
    if (tree.size > capacity) {
      tree.descendingIterator.toIterator.
        take(tree.size - capacity).toList.
        foreach(tree.remove)
    }
  }

  def getHeapCapacity: Int = LOCK.synchronized(capacity)

  def setHeapCapacity(size: Int) {
    LOCK.synchronized {
      this.capacity = size
      removeExceed()
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
      tree.add(item)
      removeExceed()
    }
  }

  def addItems(items: Iterable[T]) {
    LOCK.synchronized {
      tree.addAll(items)
      removeExceed()
    }
  }

}
