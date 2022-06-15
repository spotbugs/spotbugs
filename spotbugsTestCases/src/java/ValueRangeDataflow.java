public class ValueRangeDataflow {

    // Simple comparisons

    int eq(int n) {
        if (n == 100) {
            return n;
        }
        return 0;
    }

    int ne(int n) {
        if (n != 100) {
            return n;
        }
        return 0;
    }

    int lt(int n) {
        if (n < 100) {
            return n;
        }
        return 0;
    }

    int le(int n) {
        if (n <= 100) {
            return n;
        }
        return 0;
    }

    int gt(int n) {
        if (n > 100) {
            return n;
        }
        return 0;
    }

    int ge(int n) {
        if (n >= 100) {
            return n;
        }
        return 0;
    }

    // Type range tests

    boolean eqBoolean(boolean b) {
        if (b) {
            return b;
        }
        return false;
    }

    byte eqByte(byte b) {
        if (b == 100) {
            return b;
        }
        return 0;
    }

    short eqShort(short n) {
        if (n == 100) {
            return n;
        }
        return 0;
    }

    long eqLong(long n) {
        if (n == 100l) {
            return n;
        }
        return 0;
    }

    char eqChar(char c) {
        if (c == 'd') {
            return c;
        }
        return 0;
    }

    // Merging ranges

    int fullRange(int n) {
       int m = 0;
        if (n < 100) {
            m = 200;
        }
        return n + m;
    }

    int partialRange(int n) {
        if (n < -1000 || n > 1000) {
            return 0;
        }

        int m = 0;
         if (n < 100) {
             m = 200;
         }
         return n + m;
     }

     // Other parameter not checked in a branch

     int otherRange(int n, short m) {
        int k = 0;
         if (n < 100) {
             k = 200;
         } else {
             if (m < 100) {
                 k = 300;
             }
         }
         return n + m + k;
     }
}
