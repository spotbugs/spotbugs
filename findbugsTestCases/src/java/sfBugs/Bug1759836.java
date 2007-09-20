package sfBugs;

/**
 * 
 * Submitted By: Tor Norbye
 * Summary:
 * 
 * I just tracked down a bug in my code where I was calling
 * java.lang.ProcessBuilder's redirectErrorStream method.
 * 
 * ProcessBuilder pb = new ProcessBuilder(...)
 * pb.redirectErrorStream();
 * 
 * I had just used code completion in the IDE to find the redirectErrorStream
 * method, which I (incorrectly) believed would cause the standard error to be
 * merged with standard output such that I could just read the output to get
 * "all" the output from the process, including stderr.
 * 
 * In any case, it turns out that ProcessBuilder.redirectErrorStream() is a
 * GETTER method - it just returns true/false depending on whether the output
 * is currently redirected. Yes, a spectacularly poorly named method.
 * 
 * The correct method to be called is
 * 
 * pb.redirectErrorStream(true);
 * 
 * In any case, I thought findbugs should be able to find methods like these.
 * I know you already detect some library methods that are known to have no
 * side effects and where calling it without looking at the return value is a
 * sign of incorrect API usage. E.g. the above is very similar in my mind to
 * 
 * mystring.trim();
 * 
 * which findbugs -does- warn about.
 * 
 * Therefore, I'd like to see findbugs identify the above redirectErrorStream
 * bug. I tried with findbugs 1.2.1 on my source and it did not complain
 * about it.
 * 
 * I took a brief look at the source code in case a patch would be trivial,
 * but the dumb method detection doesn't seem to be table-driven; I saw a lot
 * of specialized logic dealing with invokevirtuals and such so I'm afraid my
 * patch would be wrong anyway. It would seem to me that this (detecting
 * methods that are known to have no side effects) should be driven off of an
 * API table just like the jdkBaseNonnullReturn.db file (apologies if it
 * already is and I read the code wrong).
 */
public class Bug1759836 {
    ////
    public static void main(String args[]) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream();
    }
}
