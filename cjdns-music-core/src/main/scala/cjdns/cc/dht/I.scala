package cjdns.cc.dht

import java.net.{InetAddress, Inet6Address, InetSocketAddress}
import org.apache.commons.codec.binary.Hex
import com.google.protobuf.ByteString
import cjdns.util.collection.BitSet
import java.util
import cjdns.util.Permutation

/**
 * User: willzyx
 * Date: 05.07.13 - 0:28
 */
class I(private val bitset: BitSet) extends Comparable[I] {

  def toAddress =
    new InetSocketAddress(
      InetAddress.getByAddress(
        I.IP_SHUFFLE.reverseTransform(bitset.toByteArray)
      ),
      PORT
    )

  def toProto = ByteString.copyFrom(bitset.toByteArray)

  /* */

  def ?(i: I): Int = {
    I.BITS_COUNT - {
      Iterator.range(0, I.BITS_COUNT).
        takeWhile(j => !(bitset.get(j) ^ i.bitset.get(j))).
        size
    }
  }

  override def hashCode = bitset.hashCode()

  override def equals(obj: Any) =
    obj != null &&
      obj.isInstanceOf[I] &&
      obj.asInstanceOf[I].bitset == bitset

  override def toString =
    new String(Hex.encodeHex(util.Arrays.copyOf(bitset.toByteArray, I.SIZE)))

  def compareTo(i: I): Int = {
    Iterator.range(0, I.BITS_COUNT).map(j => {
      val a1 = bitset.get(j)
      val a2 = i.bitset.get(j)
      if (a1 ^ a2) {
        if (a2) 1 else -1
      } else 0
    }).find(_ != 0).getOrElse(0)
  }
}

object I {
  val SIZE = 16
  val BITS_COUNT = SIZE * 8
  val IP_SHUFFLE =
    new Permutation(
      14, 8, 11, 15, 13, 9, 10, 12,
      6, 3, 4, 2, 0, 7, 5, 1
    )

  def apply(bytes: Array[Byte]): I =
    if (bytes.length == SIZE)
      new I(BitSet.valueOf(bytes))
    else
      throw new IllegalArgumentException

  def apply(address: Inet6Address): I =
    I(IP_SHUFFLE.transform(address.getAddress))

  def fromProto(buffer: ByteString): I = I(buffer.toByteArray)

  def fromAddress(address: InetAddress) =
    address match {
      case v: Inet6Address => apply(v)
      case _ => throw new IllegalArgumentException
    }
}