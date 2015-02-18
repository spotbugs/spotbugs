package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1359 {
    /* JumpInfo for this method was not properly converged, thus
     * the variables defined near the end of the method was unnoticed 
     */
    @ExpectWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    public int constSum() {
        int sum = 0;
        int l1 = 10;
        for (int i = 0; i < l1; i++)
            sum += i;
        int l2 = 10;
        for (int i = 0; i < l2; i++)
            sum += i;
        int l3 = 10;
        for (int i = 0; i < l3; i++)
            sum += i;
        int l4 = 10;
        for (int i = 0; i < l4; i++)
            sum += i;
        int l5 = 10;
        for (int i = 0; i < l5; i++)
            sum += i;
        int l6 = 10;
        for (int i = 0; i < l6; i++)
            sum += i;
        int l7 = 10;
        for (int i = 0; i < l7; i++)
            sum += i;
        int l8 = 10;
        for (int i = 0; i < l8; i++)
            sum += i;
        int l9 = 10;
        for (int i = 0; i < l9; i++)
            sum += i;
        int l10 = 10;
        for (int i = 0; i < l10; i++)
            sum += i;
        int l11 = 10;
        for (int i = 0; i < l11; i++)
            sum += i;
        int l12 = 10;
        for (int i = 0; i < l12; i++)
            sum += i;
        int l13 = 10;
        for (int i = 0; i < l13; i++)
            sum += i;
        int l14 = 10;
        for (int i = 0; i < l14; i++)
            sum += i;
        int l15 = 10;
        for (int i = 0; i < l15; i++)
            sum += i;
        int l16 = 10;
        for (int i = 0; i < l16; i++)
            sum += i;
        int l17 = 10;
        for (int i = 0; i < l17; i++)
            sum += i;
        int l18 = 10;
        for (int i = 0; i < l18; i++)
            sum += i;
        int l19 = 10;
        for (int i = 0; i < l19; i++)
            sum += i;
        int l20 = 10;
        for (int i = 0; i < l20; i++)
            sum += i;
        int l21 = 10;
        for (int i = 0; i < l21; i++)
            sum += i;
        int l22 = 10;
        for (int i = 0; i < l22; i++)
            sum += i;
        int l23 = 10;
        for (int i = 0; i < l23; i++)
            sum += i;
        int l24 = 10;
        for (int i = 0; i < l24; i++)
            sum += i;
        int l25 = 10;
        for (int i = 0; i < l25; i++)
            sum += i;
        int l26 = 10;
        for (int i = 0; i < l26; i++)
            sum += i;
        int l27 = 10;
        for (int i = 0; i < l27; i++)
            sum += i;
        int l28 = 10;
        for (int i = 0; i < l28; i++)
            sum += i;
        int l29 = 10;
        for (int i = 0; i < l29; i++)
            sum += i;
        int l30 = 10;
        for (int i = 0; i < l30; i++)
            sum += i;
        int l31 = 10;
        for (int i = 0; i < l31; i++)
            sum += i;
        int l32 = 10;
        for (int i = 0; i < l32; i++)
            sum += i;
        int l33 = 10;
        for (int i = 0; i < l33; i++)
            sum += i;
        int l34 = 10;
        for (int i = 0; i < l34; i++)
            sum += i;
        int l35 = 10;
        for (int i = 0; i < l35; i++)
            sum += i;
        int l36 = 10;
        for (int i = 0; i < l36; i++)
            sum += i;
        int l37 = 10;
        for (int i = 0; i < l37; i++)
            sum += i;
        int l38 = 10;
        for (int i = 0; i < l38; i++)
            sum += i;
        int l39 = 10;
        for (int i = 0; i < l39; i++)
            sum += i;
        int l40 = 10;
        for (int i = 0; i < l40; i++)
            sum += i;
        int l41 = 10;
        for (int i = 0; i < l41; i++)
            sum += i;
        int l42 = 10;
        for (int i = 0; i < l42; i++)
            sum += i;
        int l43 = 10;
        for (int i = 0; i < l43; i++)
            sum += i;
        int l44 = 10;
        for (int i = 0; i < l44; i++)
            sum += i;
        int l45 = 10;
        for (int i = 0; i < l45; i++)
            sum += i;
        int l46 = 10;
        for (int i = 0; i < l46; i++)
            sum += i;
        int l47 = 10;
        for (int i = 0; i < l47; i++)
            sum += i;
        int l48 = 10;
        for (int i = 0; i < l48; i++)
            sum += i;
        int l49 = 10;
        for (int i = 0; i < l49; i++)
            sum += i;
        // Just any bug involving OpcodeStack and new variables here
        int nonNegative = sum & 0xFF;
        if(nonNegative == -1)
            return 0;
        return sum;
    }

}
