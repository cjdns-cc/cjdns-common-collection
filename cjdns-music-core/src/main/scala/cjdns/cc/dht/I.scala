package cjdns.cc.dht

import java.net.{InetAddress, Inet6Address, InetSocketAddress}
import java.util
import org.apache.commons.codec.binary.Hex
import com.google.protobuf.ByteString

/**
 * User: willzyx
 * Date: 05.07.13 - 0:28
 */
class I(private val bitset: util.BitSet) {

  def toAddress =
    new InetSocketAddress(
      InetAddress.getByAddress(bitset.toByteArray),
      PORT
    )

  def toProto = ByteString.copyFrom(bitset.toByteArray)

  /* */

  def ^(i: I): I = {
    val buffer = new util.BitSet(I.BITS_COUNT)
    buffer.or(bitset)
    buffer.xor(i.bitset)
    new I(buffer)
  }

  private def length: Int =
    Iterator.range(0, I.BITS_COUNT).
      map(bitset.get).
      takeWhile(!_).
      size

  def ?(i: I): Int = (this ^ i).length

  override def hashCode = bitset.hashCode()

  override def equals(obj: Any) =
    obj != null &&
      obj.isInstanceOf[I] &&
      obj.asInstanceOf[I].bitset == bitset

  override def toString =
    new String(Hex.encodeHex(util.Arrays.copyOf(bitset.toByteArray, I.SIZE)))
}

object I {
  val SIZE = 16
  val BITS_COUNT = SIZE * 8

  def apply(bytes: Array[Byte]): I =
    if (bytes.length == SIZE)
      new I(util.BitSet.valueOf(bytes))
    else
      throw new IllegalArgumentException

  def apply(address: Inet6Address): I = I(address.getAddress)

  def fromProto(buffer: ByteString): I = I(buffer.toByteArray)

  def fromAddress(address: InetAddress) =
    address match {
      case v: Inet6Address => apply(v)
      case _ => throw new IllegalArgumentException
    }
}