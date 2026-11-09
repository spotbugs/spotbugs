/* SPDX-License-Identifier: LGPL-2.1-or-later */
package ghIssues;

public class Issue4436 {
    public int ordinary(int i) {
        StringBuilder sb = new StringBuilder();
        if (i == 19) {
            sb.append('A');
        } else {
            sb.append('A');
        }
        return sb.length();
    }

    public int nestedThen(int i) {
        StringBuilder sb = new StringBuilder();
        if (i != 1) {
            if (i == 19) {
                sb.append('A');
            } else {
                sb.append('A');
            }
        } else {
            sb.append('B');
        }
        return sb.length();
    }

    public int nestedElse(int i) {
        StringBuilder sb = new StringBuilder();
        if (i == 1) {
            sb.append('B');
        } else {
            if (i == 19) {
                sb.append('A');
            } else {
                sb.append('A');
            }
        }
        return sb.length();
    }

    public int unequalInner(int i) {
        StringBuilder sb = new StringBuilder();
        if (i != 1) {
            if (i == 19) {
                sb.append('A');
            } else {
                sb.append('B');
            }
        } else {
            sb.append('C');
        }
        return sb.length();
    }

    public int extraElseStatement(int i) {
        StringBuilder sb = new StringBuilder();
        if (i != 1) {
            if (i == 19) {
                sb.append('A');
            } else {
                sb.append('A');
                sb.append('C');
            }
        } else {
            sb.append('B');
        }
        return sb.length();
    }

    public int differentJoin(int i) {
        StringBuilder sb = new StringBuilder();
        for (; i > 0; i--) {
            if (i != 1) {
                if (i == 19) {
                    sb.append('A');
                    break;
                } else {
                    sb.append('A');
                    continue;
                }
            } else {
                sb.append('B');
            }
        }
        return sb.length();
    }
}
