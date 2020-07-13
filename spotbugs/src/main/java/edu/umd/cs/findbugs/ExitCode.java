package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class ExitCode {
    private static final Logger LOG = LoggerFactory.getLogger(ExitCode.class);

    private final int exitCode;
    @NonNull
    private final String signalName;

    private ExitCode(int exitCode, @NonNull String signalName) {
        this.exitCode = exitCode;
        this.signalName = Objects.requireNonNull(signalName);
    }

    public int getExitCode() {
        return exitCode;
    }

    @NonNull
    public String getSignalName() {
        return signalName;
    }

    public static ExitCode from(int errors, int missingClasses, int bugs) {
        int exitCode = 0;
        LOG.info("Calculating exit code...");
        if (errors > 0) {
            exitCode |= ExitCodes.ERROR_FLAG;
            LOG.debug("Setting 'errors encountered' flag ({})", ExitCodes.ERROR_FLAG);
        }
        if (missingClasses > 0) {
            exitCode |= ExitCodes.MISSING_CLASS_FLAG;
            LOG.debug("Setting 'missing class' flag ({})", ExitCodes.MISSING_CLASS_FLAG);
        }
        if (bugs > 0) {
            exitCode |= ExitCodes.BUGS_FOUND_FLAG;
            LOG.debug("Setting 'bugs found' flag ({})", ExitCodes.BUGS_FOUND_FLAG);
        }
        LOG.debug("Exit code set to: {}", exitCode);

        return new ExitCode(exitCode, getSignalName(exitCode));
    }

    private static String getSignalName(int exitCode) {
        if (exitCode == 0) {
            return "SUCCESS";
        }

        List<String> list = new ArrayList<>();
        if ((exitCode | ExitCodes.ERROR_FLAG) > 0) {
            list.add("ERROR");
        }
        if ((exitCode | ExitCodes.MISSING_CLASS_FLAG) > 0) {
            list.add("MISSING CLASS");
        }
        if ((exitCode | ExitCodes.BUGS_FOUND_FLAG) > 0) {
            list.add("BUGS FOUND");
        }

        return list.isEmpty() ? "UNKNOWN" : list.stream().collect(Collectors.joining(","));
    }
}
