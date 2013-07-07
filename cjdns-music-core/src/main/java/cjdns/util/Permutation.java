package cjdns.util;

/**
 * User: willzyx
 * Date: 07.07.13 - 20:44
 */
public final class Permutation {
    public final int size;
    private final int[] a;

    public Permutation(int... a) {
        this.size = a.length;
        this.a = a;
    }

    private int cycle(int i) {
        int k = 0;
        int j = i;
        do {
            j = a[j];
            k++;
        } while (j != i);
        return k;
    }

    public boolean isSingleCycle() {
        return cycle(0) == size;
    }

    public byte[] transform(byte[] buffer) {
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) {
            out[i] = buffer[a[i]];
        }
        return out;
    }

    public byte[] reverseTransform(byte[] buffer) {
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) {
            out[a[i]] = buffer[i];
        }
        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean f = false;
        for (int i = 0; i < size; i++) {
            if (f) {
                sb.append(", ");
            }
            sb.append(a[i]);
            f = true;
        }
        sb.append(")");
        return sb.toString();
    }

}
