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

  def getHeapCapacity: Int = LOCK.synchronized(capacity)

  def setHeapCapacity(size: Int) {
    LOCK.synchronized {
      this.capacity = size
      if (tree.size > size) {
        tree.toIterator.drop(size).toList.
          foreach(tree.remove)
      }
    }
  }

  def getHeapSize: Int = LOCK.synchronized(tree.size)

  def getItems: List[T] =
    LOCK.synchronized(tree.toList)

  def getTopItems(count: Int): List[T] =
    LOCK.synchronized(tree.toIterator.take(count).toList)

}
