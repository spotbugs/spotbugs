package ghIssues;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Issue 4398: a non-static inner class of a multi-threaded class is analyzed
 * because it can reach the enclosing instance through {@code this$0}, but its
 * own fields, such as the cursor of an iterator or a parser, must not be
 * reported unless the inner class is multi-threaded itself. The writes of a
 * multi-threaded inner class, such as a {@link Runnable}, are still checked,
 * even to a field of a class that is not multi-threaded.
 */
public class Issue4398 {
    private final Object lock = new Object();
    private final String pattern;
    private int count;

    public Issue4398(String pattern) {
        this.pattern = pattern;
    }

    public void increment() {
        synchronized (lock) {
            count++;
        }
    }

    public Iterator<Character> iterator() {
        return new Iterator<Character>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < pattern.length();
            }

            @Override
            public Character next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return pattern.charAt(index++);
            }
        };
    }

    public final class PatternCursor {
        private int position;

        public boolean hasNext() {
            return position < pattern.length();
        }

        public char next() {
            return pattern.charAt(position++);
        }

        public void skip() {
            position++;
        }
    }

    public final class Session {
        private boolean closed;

        public boolean isClosed() {
            return closed;
        }

        public final class Closer implements Runnable {
            @Override
            public void run() {
                closed = true;
                increment();
            }
        }
    }

    public static class Heartbeat {
        private boolean stopped;

        public final class Pinger implements Runnable {
            @Override
            public void run() {
                while (!stopped) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            public void stop() {
                stopped = true;
            }
        }
    }
}
