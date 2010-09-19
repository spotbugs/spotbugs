package sfBugs;

import java.util.HashMap;
import java.util.Map;

public class Bug1933922 {

    private Map<String, String> programs = new HashMap<String, String>();

    private Map<String, Thread> compiling = new HashMap<String, Thread>();

    public static void main(String args[]) {
        String file = args[0];
        String prog = args[1];
        (new Bug1933922()).foo(file, prog);
    }

    public String foo(String file, String prog) {
        try {
            // Compiler compiler = new Compiler(this, file);
            // Program prog = compiler.compile();
            programs.put(file, prog);
            return prog;
        } finally {
            synchronized (compiling) {
                compiling.remove(file);
                compiling.notifyAll();
            }
        }
    }

    public String getProgram(String file, boolean load) {
        try {
            synchronized (compiling) {
                for (;;) {
                    // Thread other = compiling.get(file);
                    Thread other = Thread.currentThread();
                    if (other == null)
                        break;
                    if (other == Thread.currentThread())
                        throw new IllegalArgumentException("Recursive compilation in " + file);
                    /* XXX This can deadlock. */
                    compiling.wait();
                }
                String program = programs.get(file);
                if (program != null)
                    return program;
                if (!load)
                    return null;
                compiling.put(file, Thread.currentThread());
            }
            try {
                // Compiler compiler = new Compiler(this, file);
                // LpcProgram prog = compiler.compile();
                String compiler = "Hello";
                String prog = "world";
                programs.put(file, prog);
                return prog;
            } finally {
                synchronized (compiling) {
                    compiling.remove(file);
                    compiling.notifyAll(); // The submitter's app yields a
                                           // warning here;
                                           // I haven't been able to get this
                                           // example
                                           // to trigger the same warning.
                }
            }
        }
        // catch (IOException e) {
        // throw new IllegalArgumentException(e);
        // }
        catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
        // catch (CompilerException e) {
        // throw new IllegalArgumentException(e);
        // }
    }

}
