package edu.umd.cs.findbugs.ba.npe;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.ba.Location;

/**
 * Collected information about a single value number observed at one or more
 * locations to be both definitely-null and unconditionally dereferenced.
 */
public class NullValueUnconditionalDeref {
    private boolean alwaysOnExceptionPath;

    private boolean alwaysFieldValue;

    private boolean alwaysMethodReturnValue;

    private boolean alwaysReadlineValue;

    private final Set<Location> derefLocationSet;

    public NullValueUnconditionalDeref() {
        this.alwaysOnExceptionPath = true;
        this.alwaysMethodReturnValue = true;
        this.alwaysFieldValue = true;
        this.alwaysReadlineValue = true;
        this.derefLocationSet = new HashSet<Location>();
    }

    /**
     * @param isNullValue
     * @param unconditionalDerefLocationSet
     */
    public void add(IsNullValue isNullValue, Set<Location> unconditionalDerefLocationSet) {
        if (!isNullValue.isException()) {
            alwaysOnExceptionPath = false;
        }
        if (!isNullValue.isFieldValue()) {
            alwaysFieldValue = false;
        }
        if (!isNullValue.isReturnValue()) {
            alwaysMethodReturnValue = false;
        }
        if (!isNullValue.isReadlineValue()) {
            alwaysReadlineValue = false;
        }
        derefLocationSet.addAll(unconditionalDerefLocationSet);
    }

    /**
     * @return Returns the derefLocationSet.
     */
    public Set<Location> getDerefLocationSet() {
        return derefLocationSet;
    }

    /**
     * @return Returns the alwaysOnExceptionPath.
     */
    public boolean isAlwaysOnExceptionPath() {
        return alwaysOnExceptionPath;
    }

    /**
     * @return Returns the alwaysMethodReturnValue.
     */
    public boolean isMethodReturnValue() {
        return alwaysMethodReturnValue;
    }

    /**
     * @return Returns the alwaysMethodReturnValue.
     */
    public boolean isReadlineValue() {
        return alwaysReadlineValue;
    }

    /**
     * @return Returns the alwaysFieldValue.
     */
    public boolean isFieldValue() {
        return alwaysFieldValue;
    }
}
