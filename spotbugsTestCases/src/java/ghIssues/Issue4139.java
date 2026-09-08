package ghIssues;

/**
 * UR_UNINIT_READ must be reported consistently for a compound assignment {@code |=}
 * and its equivalent explicit form {@code x = x | ...}, since both read the field
 * before it is assigned in the constructor. See
 * <a href="https://github.com/spotbugs/spotbugs/issues/4139">issue 4139</a>.
 */
public class Issue4139 {

    private static final int BIT0 = 1;

    protected int m_iType;

    protected int counter;

    public Issue4139(boolean available) {
        // Compound form: javac emits ALOAD_0; DUP; GETFIELD m_iType; ...; IOR; PUTFIELD m_iType.
        // The DUP keeps a copy of 'this' for the PUTFIELD, but the GETFIELD still reads
        // 'this.m_iType' before assignment. Before the fix this was NOT reported because the
        // DUP reset the detector's thisOnTOS tracking.
        m_iType |= available ? BIT0 : 0;
    }

    public Issue4139(int seed) {
        // Explicit form (control case): already reported before the fix.
        m_iType = m_iType | seed;
    }

    public Issue4139(short delta) {
        // Compound '+=' on a second field: same ALOAD_0; DUP; GETFIELD pattern as '|='.
        // Locks the fix for the whole compound-assignment family, not only '|='.
        counter += delta;
    }

    public Issue4139() {
        // Post-increment 'counter++': same ALOAD_0; DUP; GETFIELD pattern.
        counter++;
    }
}
