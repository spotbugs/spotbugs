package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2857782 {
    @NoWarning("IL_INFINITE_LOOP")
    public void bug1() {
        int nc = 20;
        while (nc-- > 0) {
        }
    }

    @NoWarning("IL_INFINITE_LOOP")
    public void bug2() {
        int nc = 20;
        while (nc > 0) {
            nc--;
        }
    }

    @NoWarning("IL_INFINITE_LOOP")
    public static String convertToString(int val, int len) {
        StringBuilder sb = new StringBuilder();
        sb.append(val);
        int dataLen = sb.length();
        if (dataLen < len) {
            for (int i = dataLen; i < len; i++) {
                sb.insert(0, 0);
            }
        } else if (dataLen > len) {
            sb.delete(0, dataLen - len);
        }
        return sb.toString();
    }

}
