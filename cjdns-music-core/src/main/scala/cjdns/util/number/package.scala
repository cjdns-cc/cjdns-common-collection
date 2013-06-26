package cjdns.util

import util.Try

/**
 * User: willzyx
 * Date: 26.06.13 - 23:18
 */
package object number {
  def parseInt(s: String) = Try(s.toInt).toOption
}
