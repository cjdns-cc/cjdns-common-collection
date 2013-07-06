package cjdns.util.collection;

import cjdns.cjdns_music.Model.Bloom;
import com.google.protobuf.ByteString;

import java.util.Arrays;

/**
 * User: willzyx
 * Date: 29.06.13 - 23:54
 */
public final class BloomFilter<T> {
    public static final int MIN_SIZE = 64;

    public static interface Hash<T> {
        public long get(T obj);
    }

    private final Hash<T> hash;
    private final int[] vector;
    private final BitSet bitset;
    private final int size;
    private final int mask;

    private BloomFilter(int[] vector, Hash<T> hash, byte[] bitset) {
        int size = bitset.length * 8;
        if (size < MIN_SIZE) {
            throw new IllegalArgumentException("bitset too short");
        }
        if ((size & (size - 1)) != 0) {
            throw new IllegalArgumentException("illegal bitset size");
        }
        this.hash = hash;
        this.vector = Arrays.copyOf(vector, vector.length);
        this.bitset = BitSet.valueOf(bitset);
        this.size = size;
        this.mask = size - 1;
    }

    private BloomFilter(int[] vector, Hash<T> hash, int size) {
        if (size < MIN_SIZE) {
            size = MIN_SIZE;
        }
        if ((size & (size - 1)) != 0) {
            size = size << 1;
            while ((size & (size - 1)) != 0) {
                size = size & (size - 1);
            }
        }
        this.hash = hash;
        this.vector = Arrays.copyOf(vector, vector.length);
        this.bitset = new BitSet(size);
        this.size = size;
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
        builder.setBitset(
                ByteString.copyFrom(
                        Arrays.copyOf(
                                bitset.toByteArray(),
                                size >> 3
                        )
                )
        );
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

    public static int getVectorLength(int size, int count) {
        return Math.max((int) Math.round(Math.log(2) * size / count), 1);
    }

    public static int getSize(int count, double probability) {
        if (probability <= 0 || probability >= 1) {
            throw new IllegalArgumentException("probability out of range");
        }
        long a = Math.round((-count * Math.log(probability)) / Math.pow(Math.log(2), 2));
        if (a >= MIN_SIZE) {
            while ((a & (a - 1)) > 0) {
                a = a & (a - 1);
            }
            return (int) (a << 1);
        } else {
            return MIN_SIZE;
        }
    }

}
