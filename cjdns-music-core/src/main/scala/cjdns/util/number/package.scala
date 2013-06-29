package cjdns.util

import util.Try

/**
 * User: willzyx
 * Date: 26.06.13 - 23:18
 */
package object number {
  val primes: Stream[Int] =
    2 #:: 3 #:: Stream.from(4).filterNot(i => primes.takeWhile(j => (j * j <= i)).exists(i % _ == 0))

  def parseInt(s: String) = Try(s.toInt).toOption
}
