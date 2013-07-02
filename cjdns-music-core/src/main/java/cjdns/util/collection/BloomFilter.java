package cjdns.util.collection;

import cjdns.cjdns_music.Model.Bloom;
import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.BitSet;

/**
 * User: willzyx
 * Date: 29.06.13 - 23:54
 */
public final class BloomFilter<T> {
    public static interface Hash<T> {
        public long get(T obj);
    }

    private final Hash<T> hash;
    private final int[] vector;
    private final BitSet bitset;
    private final int mask;

    private BloomFilter(int[] vector, Hash<T> hash, byte[] bitset) {
        int size = bitset.length * 8;
        assert ((size & (size - 1)) == 0);
        this.hash = hash;
        this.vector = Arrays.copyOf(vector, vector.length);
        this.bitset = BitSet.valueOf(bitset);
        this.mask = size - 1;
    }

    private BloomFilter(int[] vector, Hash<T> hash, int size) {
        assert ((size & (size - 1)) == 0);
        this.hash = hash;
        this.vector = Arrays.copyOf(vector, vector.length);
        this.bitset = new BitSet(size);
        this.mask = size - 1;
    }

    public void add(T obj) {
        for (int k : vector) {
            bitset.set((int) (hash.get(obj) * k) & mask);
        }
    }

    public boolean contains(T obj) {
        for (int k : vector) {
            if (!bitset.get((int) (hash.get(obj) * k) & mask)) {
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

    public Bloom getMessage() {
        Bloom.Builder builder = Bloom.newBuilder();
        for (int k : vector) {
            builder.addFactor(k);
        }
        builder.setBitset(ByteString.copyFrom(bitset.toByteArray()));
        return builder.build();
    }

    /* STATIC INTERFACE */

    private final static Hash<Object> DEFAULT_HASH =
            new Hash<Object>() {
                @Override
                public long get(Object obj) {
                    return obj.hashCode();
                }
            };

    public static BloomFilter<Object> restore(Bloom message) {
        int[] vector = new int[message.getFactorCount()];
        for (int i = 0; i < message.getFactorCount(); i++) {
            vector[i] = message.getFactor(i);
        }
        return new BloomFilter<Object>(
                vector,
                DEFAULT_HASH,
                message.getBitset().toByteArray()
        );
    }

    public static BloomFilter<Object> getEmpty(int[] vector, int size) {
        return new BloomFilter<Object>(
                vector,
                DEFAULT_HASH,
                size
        );
    }

}
