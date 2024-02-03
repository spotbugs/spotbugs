package edu.umd.cs.findbugs.ba;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

public class ValueRangeDataflowTest extends AbstractIntegrationTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void test1() {
        System.setProperty("dataflow.classname", "edu.umd.cs.findbugs.ba.ValueRangeDataflow");
        System.setProperty("dataflow.printcfg", "true");
        performAnalysis("ValueRangeDataflow.class");
        String[] outputLines = outContent.toString().split(System.lineSeparator());
        checkOutput(outputLines);
    }

    private void checkOutput(String[] outputLines) {
        for (int i = 0; i < outputLines.length; ++i) {
            if ("Method: ValueRangeDataflow.eq(int)".equals(outputLines[i])) {
                checkEQ(outputLines, i);
            } else if ("Method: ValueRangeDataflow.ne(int)".equals(outputLines[i])) {
                checkNE(outputLines, i);
            } else if ("Method: ValueRangeDataflow.lt(int)".equals(outputLines[i])) {
                checkLT(outputLines, i);
            } else if ("Method: ValueRangeDataflow.le(int)".equals(outputLines[i])) {
                checkLE(outputLines, i);
            } else if ("Method: ValueRangeDataflow.gt(int)".equals(outputLines[i])) {
                checkGT(outputLines, i);
            } else if ("Method: ValueRangeDataflow.ge(int)".equals(outputLines[i])) {
                checkGE(outputLines, i);

            } else if ("Method: ValueRangeDataflow.eqBoolean(boolean)".equals(outputLines[i])) {
                checkEQBoolean(outputLines, i);
            } else if ("Method: ValueRangeDataflow.eqByte(byte)".equals(outputLines[i])) {
                checkEQByte(outputLines, i);
            } else if ("Method: ValueRangeDataflow.eqShort(short)".equals(outputLines[i])) {
                checkEQShort(outputLines, i);
            } else if ("Method: ValueRangeDataflow.eqLong(long)".equals(outputLines[i])) {
                checkEQLong(outputLines, i);
            } else if ("Method: ValueRangeDataflow.eqChar(char)".equals(outputLines[i])) {
                checkEQChar(outputLines, i);

            } else if ("Method: ValueRangeDataflow.fullRange(int)".equals(outputLines[i])) {
                checkFullRange(outputLines, i);
            } else if ("Method: ValueRangeDataflow.partialRange(int)".equals(outputLines[i])) {
                checkPartialRange(outputLines, i);

            } else if ("Method: ValueRangeDataflow.otherRange(int, short)".equals(outputLines[i])) {
                checkOtherRange(outputLines, i);
            }
        }
    }

    private void checkEQ(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + Integer.MIN_VALUE + ", 99\\]\\+\\[101, " +
                        Integer.MAX_VALUE + "\\].*Branch data: Value number: \\d,; true condition: n \\!\\= 100; false condition: n \\=\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\{100\\}.*" +
                        "Branch data: Value number: \\d,; true condition: n \\=\\= 100; false condition: n \\!\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkNE(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\{100\\}.*" +
                        "Branch data: Value number: \\d,; true condition: n \\=\\= 100; false condition: n \\!\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\[" + Integer.MIN_VALUE +
                        ", 99\\]\\+\\[101, " + Integer.MAX_VALUE +
                        "\\].*Branch data: Value number: \\d,; true condition: n \\!\\= 100; false condition: n \\=\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkLT(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[100, " + Integer.MAX_VALUE +
                        "\\].*Branch data: Value number: \\d,; true condition: n \\>\\= 100; false condition: n \\< 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\[" + Integer.MIN_VALUE +
                        ", 99\\].*Branch data: Value number: \\d,; true condition: n \\< 100; false condition: n \\>\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkLE(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[101, " + Integer.MAX_VALUE +
                        "\\].*Branch data: Value number: \\d,; true condition: n \\> 100; false condition: n \\<\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\[" + Integer.MIN_VALUE +
                        ", 100\\].*Branch data: Value number: \\d,; true condition: n \\<\\= 100; false condition: n \\> 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkGT(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + Integer.MIN_VALUE +
                        ", 100\\].*Branch data: Value number: \\d,; true condition: n \\<\\= 100; false condition: n \\> 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\[101, " + Integer.MAX_VALUE +
                        "\\].*Branch data: Value number: \\d,; true condition: n \\> 100; false condition: n \\<\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkGE(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + Integer.MIN_VALUE +
                        ", 99\\].*Branch data: Value number: \\d,; true condition: n \\< 100; false condition: n \\>\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\[100, " + Integer.MAX_VALUE +
                        "\\].*Branch data: Value number: \\d,; true condition: n \\>\\= 100; false condition: n \\< 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkEQBoolean(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\{0\\}.*" +
                        "Branch data: Value number: \\d,; true condition: b \\=\\= 0; false condition: b \\!\\= 0"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\{1\\}.*" +
                        "Branch data: Value number: \\d,; true condition: b \\!\\= 0; false condition: b \\=\\= 0"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkEQByte(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + Byte.MIN_VALUE + ", 99\\]\\+\\[101, " +
                        Byte.MAX_VALUE + "\\].*Branch data: Value number: \\d,; true condition: b \\!\\= 100; false condition: b \\=\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\{100\\}.*" +
                        "Branch data: Value number: \\d,; true condition: b \\=\\= 100; false condition: b \\!\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkEQShort(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + Short.MIN_VALUE + ", 99\\]\\+\\[101, " +
                        Short.MAX_VALUE + "\\].*Branch data: Value number: \\d,; true condition: n \\!\\= 100; false condition: n \\=\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\{100\\}.*" +
                        "Branch data: Value number: \\d,; true condition: n \\=\\= 100; false condition: n \\!\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkEQLong(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + Long.MIN_VALUE + ", 99\\]\\+\\[101, " +
                        Long.MAX_VALUE + "\\].*Branch data: Value number: \\d,; true condition: n \\!\\= 100; false condition: n \\=\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\{100\\}.*" +
                        "Branch data: Value number: \\d,; true condition: n \\=\\= 100; false condition: n \\!\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkEQChar(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "EDGE(1)".equals(outputLines[i].substring(2, 9))) {
                assertTrue(outputLines[i].matches("  EDGE\\(1\\) type IFCMP.*Variable ranges:.*\\[" + (int) Character.MIN_VALUE +
                        ", 99\\]\\+\\[101, " + (int) Character.MAX_VALUE +
                        "\\].*Branch data: Value number: \\d,; true condition: c \\!\\= 100; false condition: c \\=\\= 100"));
                assertTrue(outputLines[i + 1].matches("  EDGE\\(2\\) type FALL_THROUGH.*Variable ranges:.*\\{100\\}.*" +
                        "Branch data: Value number: \\d,; true condition: c \\=\\= 100; false condition: c \\!\\= 100"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkFullRange(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "BASIC BLOCK: 3".equals(outputLines[i].substring(0, 14))) {
                assertTrue(outputLines[i].matches("BASIC BLOCK: 3 Variable ranges:.*\\[" + Integer.MIN_VALUE + ", " +
                        Integer.MAX_VALUE + "\\].*"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkPartialRange(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "BASIC BLOCK: 6".equals(outputLines[i].substring(0, 14))) {
                assertTrue(outputLines[i].matches("BASIC BLOCK: 6 Variable ranges:.*\\[-1000, 1000\\].*"));
                return;
            }
        }
        Assert.fail();
    }

    private void checkOtherRange(String[] outputLines, int methodLine) {
        for (int i = methodLine + 4; i < outputLines.length; ++i) {
            if (outputLines[i].length() >= 9 && "BASIC BLOCK: 5".equals(outputLines[i].substring(0, 14))) {
                assertTrue(outputLines[i].matches("BASIC BLOCK: 5 Variable ranges:.*\\[" + Integer.MIN_VALUE + ", " +
                        Integer.MAX_VALUE + "\\].*"));
                assertTrue(outputLines[i].matches("BASIC BLOCK: 5 Variable ranges:.*\\[" + Short.MIN_VALUE + ", " +
                        Short.MAX_VALUE + "\\].*"));
                return;
            }
        }
        Assert.fail();
    }
}
