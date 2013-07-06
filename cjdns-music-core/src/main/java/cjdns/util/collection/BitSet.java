package cjdns.util.collection;

import java.util.Arrays;

/**
 * User: willzyx
 * Date: 06.07.13 - 22:54
 */
public final class BitSet {
    private final int wordCount;
    public final int length;
    private final long[] words;

    public BitSet(int length) {
        if ((length & 0x3F) != 0) {
            throw new IllegalArgumentException();
        }
        this.wordCount = length >> 6;
        this.length = length;
        this.words = new long[this.wordCount];
    }

    private BitSet(long[] words) {
        this.wordCount = words.length;
        this.length = this.wordCount << 6;
        this.words = words;
    }

    @Override
    public int hashCode() {
        long k = 0;
        for (int i = 0; i < wordCount; i++) {
            k = k * 37 + words[i];
        }
        return (int) (k ^ (k >> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof BitSet)) return false;
        BitSet bitset = (BitSet) obj;
        if (wordCount != bitset.wordCount) return false;
        for (int i = 0; i < wordCount; i++) {
            if (words[i] != bitset.words[i]) return false;
        }
        return true;
    }

    public void set(int i) {
        words[i >> 6] |= (1L << i);
    }

    public boolean get(int i) {
        return (words[i >> 6] & (1L << i)) != 0;
    }

    /* */

    public BitSet xor(BitSet bitset) {
        for (int i = 0; (i < bitset.wordCount) && (i < wordCount); i++) {
            words[i] ^= bitset.words[i];
        }
        return this;
    }

    /* */

    public byte[] toByteArray() {
        byte[] buffer = new byte[wordCount << 3];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (words[i >> 3] >>> ((i & 7) << 3));
        }
        return buffer;
    }

    /* */

    public static BitSet valueOf(byte[] buffer) {
        return valueOf(buffer, buffer.length << 3);
    }

    public static BitSet valueOf(byte[] buffer, int length) {
        BitSet bits = new BitSet(length);
        for (int i = 0; i < bits.wordCount << 3; i++) {
            bits.words[i >> 3] |= (buffer[i] & 0xFFL) << ((i & 7) << 3);
        }
        return bits;
    }

    public static BitSet valueOf(BitSet bitset) {
        return new BitSet(Arrays.copyOf(bitset.words, bitset.words.length));
    }

}
