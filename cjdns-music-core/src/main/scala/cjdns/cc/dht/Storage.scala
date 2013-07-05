package cjdns.cc.dht

import collection.mutable

/**
 * User: willzyx
 * Date: 05.07.13 - 17:53
 */
class Storage {
  type Timestamp = Long

  class Entry(val i: I) extends Comparable[Entry] {
    var timestamp: Timestamp = 0L

    def checkIn() {
      this.timestamp = System.currentTimeMillis
    }

    def compareTo(o: Entry): Int = {
      if (timestamp < o.timestamp) -1 else 1
    }
  }

  private val map = mutable.LinkedHashMap.empty[(I, I), mutable.LinkedHashMap[I, Entry]]

  def put(partition: I, key: I, value: I) {
    map.getOrElseUpdate((partition, key), mutable.LinkedHashMap.empty).
      getOrElseUpdate(value, new Entry(value)).
      checkIn()
  }

  def get(partition: I, key: I) = {
    val list =
      map.get((partition, key)).
        map(_.valuesIterator.toList).
        getOrElse(List.empty).
        sortBy(identity).
        map(_.i)
    if (list.size < K)
      list
    else
      list.drop(K - list.size)
  }
}
