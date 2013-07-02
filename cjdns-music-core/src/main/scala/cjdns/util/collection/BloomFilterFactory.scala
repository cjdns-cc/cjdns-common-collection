package cjdns.util.collection

import cjdns.util.number
import util.Random

/**
 * User: willzyx
 * Date: 02.07.13 - 23:36
 */
object BloomFilterFactory {
  private val VECTOR_ITEMS = number.primes.drop(16).take(64).toList

  def newDefault(count: Int) = {
    val size =
      if (count > 1)
        BloomFilter.getSize(
          count,
          math.pow(count, -0.5)
        )
      else
        BloomFilter.MIN_SIZE
    BloomFilter.getEmpty(
      Random.shuffle(VECTOR_ITEMS).
        take(BloomFilter.getVectorLength(size, count)).
        toArray,
      size
    )
  }

}
