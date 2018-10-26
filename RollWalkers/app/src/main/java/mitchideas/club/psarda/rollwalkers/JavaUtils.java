package mitchideas.club.psarda.rollwalkers;

import java.util.Random;

public class JavaUtils {

    public static long nextLong(long origin, long bound, Random random) {
        long r = random.nextLong();
        long n = bound - origin, m = n - 1;
        if ((n & m) == 0L)  // power of two
            r = (r & m) + origin;
        else if (n > 0L) {  // reject over-represented candidates
            for (long u = r >>> 1;            // ensure nonnegative
                 u + m - (r = u % n) < 0L;    // rejection check
                 u = random.nextLong() >>> 1) // retry
                ;
            r += origin;
        }
        else {              // range not representable as long
            while (r < origin || r >= bound)
                r = random.nextLong();
        }
        return r;
    }

}
