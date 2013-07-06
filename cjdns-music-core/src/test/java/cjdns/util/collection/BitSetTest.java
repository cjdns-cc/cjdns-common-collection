package cjdns.util.collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * User: willzyx
 * Date: 07.07.13 - 0:00
 */
public class BitSetTest {
    @Test
    public void testSimple() throws Exception {
        final int K = 13;
        BitSet bits = new BitSet(997 << 6);
        for (int i = 0; i < bits.length; i++) {
            if (i % K == 0) {
                bits.set(i);
            }
        }

        bits = BitSet.valueOf(bits.toByteArray(), bits.length);

        for (int i = 0; i < bits.length; i++) {
            if (i % K == 0) {
                Assert.assertTrue(bits.get(i));
            } else {
                Assert.assertFalse(bits.get(i));
            }
        }
    }
}
