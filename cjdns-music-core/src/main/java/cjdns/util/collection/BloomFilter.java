package cjdns.util.collection;

import java.util.Arrays;
import java.util.BitSet;

/**
 * User: willzyx
 * Date: 29.06.13 - 23:54
 */
public abstract class BloomFilter<T> {
    private final int[] vector;
    private final BitSet bitset;
    private final int mask;

    protected BloomFilter(int[] vector, BitSet bitset) {
        this.vector = Arrays.copyOf(vector, vector.length);
        this.bitset = bitset;
        int size = bitset.size();
        assert ((size & (size - 1)) == 0);
        this.mask = size - 1;
    }

    protected BloomFilter(int[] vector, int size) {
        this(vector, new BitSet(size));
    }

    protected abstract long hash(T obj);

    public void add(T obj) {
        for (int k : vector) {
            bitset.set((int) (hash(obj) * k) & mask);
        }
    }

    public boolean contains(T obj) {
        for (int k : vector) {
            if (!bitset.get((int) (hash(obj) * k) & mask)) {
                return false;
            }
        }
        return true;
    }

    public int[] getVector() {
        return Arrays.copyOf(vector, vector.length);
    }

    public byte[] getByteArray() {
        return bitset.toByteArray();
    }

}
