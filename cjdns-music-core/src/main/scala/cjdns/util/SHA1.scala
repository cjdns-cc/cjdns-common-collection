package cjdns.util

import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import java.io.{FileInputStream, File}

/**
 * User: willzyx
 * Date: 26.06.13 - 22:34
 */
object SHA1 {

  def hash(file: File): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA1")
    val buffer = new Array[Byte](8 * 1024)
    val fis = new FileInputStream(file)
    var r: Int = 0
    do {
      r = fis.read(buffer)
      if (r > 0) {
        md.update(buffer, 0, r)
      }
    } while (r >= 0)
    md.digest
  }

  def hash(data: Array[Byte]): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA1")
    md.update(data)
    md.digest
  }

  def hashString(s: String): String =
    hashString(s.getBytes)

  def hashString(data: Array[Byte]): String =
    new String(Hex.encodeHex(hash(data)))

}
