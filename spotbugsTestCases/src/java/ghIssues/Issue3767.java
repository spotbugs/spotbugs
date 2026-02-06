package ghIssues;

import java.io.IOException;

public class Issue3767 {
    private int m;

    public void check(int state) {

        switch (state) {
            default:
                return;
            case 1:
                m = m + 1;
                break;
            case 2:
                break;
        }
    }
    
    public void printNumbers(int size, int type) {
        switch (type) {
            case 1:
                int  k = 1;
                while (k < size - 1) {
                    k++;
                }
                break;
            case 2:
                k = 1;
                while (k < size - 1) {
                    k++;
                }
                break;
            default:
                // should not happen
                break;
        }
    }

    final public String term(int state) throws IOException {

        switch (state) {
            case 1:{
                label_5: while (true) {
                    switch (2 * state) {
                        case 8:{
                            ;
                            break;
                        }
                        default:
                            m = 2;
                            break label_5;
                    }
                    m = 1;
                }
                break;
            }
            case 2:{
                label_6:
                while (true) {
                    switch (2 * state) {
                        case 8:{
                            ;
                            break;
                        }
                        default:
                            m = 2;
                            break label_6;
                    }
                    m = 1;
                }
                break;
            }
            default:
                m = 7;
                throw new IOException();
        }
        return "";
    }
}
