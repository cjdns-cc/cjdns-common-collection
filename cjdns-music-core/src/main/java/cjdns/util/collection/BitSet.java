package cjdns.util.collection;

/**
 * User: willzyx
 * Date: 06.07.13 - 22:54
 */
public final class BitSet {
    public final int length;
    public final long[] words;

    public BitSet(int length) {
        this.length = length;
        this.words = new long[((length - 1) >> 6) + 1];
    }

    public BitSet(long[] words, int length) {
        this.words = words;
        this.length = length;
    }

    public void set(int i) {
        words[i >> 6] |= (1L << i);
    }

    public boolean get(int i) {
        return (words[i >> 6] & (1L << i)) != 0;
    }

    /* */

    public static BitSet valueOf(byte[] buffer) {
        return valueOf(buffer, buffer.length << 3);
    }

    public static BitSet valueOf(byte[] buffer, int length) {
        BitSet bits = new BitSet(length);
        int i = ((length - 1) >> 3) + 1;
        long k = 0;
        do {
            i--;
            k <<= 8;
            k |= buffer[i] & 0xffL;
            if ((i & 7) == 0) {
                bits.words[i >> 3] = k;
            }
        } while (i != 0);
        return bits;
    }

    public byte[] toByteArray() {
        byte[] buffer = new byte[((length - 1) >> 3) + 1];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte)(words[i >> 3] >>> ((i & 7) << 3));
        }
        return buffer;
    }

}
