package ghIssues;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Issue4274 {
    public void appendCharSequence() throws FileNotFoundException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("file.txt");
            writer = writer.append(null);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void appendChar() throws FileNotFoundException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("file.txt");
            writer = writer.append('a');
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void appendSubsequence() throws FileNotFoundException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("file.txt");
            writer = writer.append("abc", 0, 2);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void appendChain() throws FileNotFoundException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("file.txt");
            writer = writer.append("abc").append('d').append("efg", 0, 2);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void closeAlias() throws FileNotFoundException {
        PrintWriter alias = null;
        try {
            PrintWriter writer = new PrintWriter("file.txt");
            alias = writer;
            alias = alias.append("abc");
        } finally {
            if (alias != null) {
                alias.close();
            }
        }
    }

    public void missingClose() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter("file.txt");
        writer = writer.append("abc");
        writer.flush();
    }

    public void closeOtherWriter() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter("file.txt");
        PrintWriter other = new PrintWriter("other.txt");
        other = other.append("abc");
        other.close();
        writer.flush();
    }

    public void appendReturnsOtherWriter() throws FileNotFoundException {
        OtherWriter writer = new OtherWriter();
        PrintWriter other = writer.append("abc");
        other.close();
    }

    public void upcastAppendReturnsOtherWriter() throws FileNotFoundException {
        PrintWriter writer = new OtherWriter();
        PrintWriter other = writer.append("abc");
        other.close();
    }

    public static class OtherWriter extends PrintWriter {
        public OtherWriter() throws FileNotFoundException {
            super("file.txt");
        }

        @Override
        public PrintWriter append(CharSequence sequence) {
            return new PrintWriter(System.out);
        }
    }
}
