package ghIssues;

public class Issue4436 {
    public int seed(int i) {
        StringBuilder sb = new StringBuilder();
        if (i == 19) {
            sb.append((char) 0x0041);
        } else {
            sb.append((char) 0x0041);
        }
        return sb.length();
    }

    public int nested(int i) {
        StringBuilder sb = new StringBuilder();
        if (i != 1) {
            if (i == 19) {
                sb.append((char) 0x0041);
            } else {
                sb.append((char) 0x0041);
            }
        } else {
            sb.append((char) 0x0042);
        }
        return sb.length();
    }

    public int nestedDifferent(int i) {
        StringBuilder sb = new StringBuilder();
        if (i != 1) {
            if (i == 19) {
                sb.append((char) 0x0041);
            } else {
                sb.append((char) 0x0043);
            }
        } else {
            sb.append((char) 0x0042);
        }
        return sb.length();
    }
}
