package cjdns.util;

/**
 * User: willzyx
 * Date: 07.07.13 - 20:44
 */
public final class Permutation {
    public final int size;
    private final int[] a;

    public Permutation(int[] a) {
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

    public int minCycle() {
        int min = size;
        for (int i = 1; i < size; i++) {
            min = Math.min(min, cycle(i));
        }
        return min;
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

    /* */

    public Permutation shuffle(int k) {
        int b[] = new int[size];
        for (int i = 0; i < size; i++) {
            b[i] = (a[i] + 1) * k % size;
        }
        return new Permutation(b);
    }

    public Permutation shift(int k) {
        int b[] = new int[size];
        for (int i = 0; i < size; i++) {
            int x = a[i] + k;
            if (x < 0) x = size - x;
            b[i] = x % size;
        }
        return new Permutation(b);
    }

    public Permutation roll() {
        int b[] = new int[size];
        for (int i = 0; i < size; i++) b[i] = a[a[i]];
        return new Permutation(b);
    }

    /* */

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

    /* */

    public static Permutation identity(int size) {
        int[] a = new int[size];
        for (int i = 0; i < size; i++) a[i] = i;
        return new Permutation(a);
    }
}
