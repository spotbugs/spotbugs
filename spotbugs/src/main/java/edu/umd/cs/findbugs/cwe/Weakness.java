package edu.umd.cs.findbugs.cwe;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A weakness represents a single weakness of the Common Weakness Enumeration (CWE). The Common Weakness Enumeration is
 * a standard taxonomy of weaknesses. This weakness is a simplification of the CWE weakness. It contains far fewer data
 * points as compared to the CWE list. In addition, it does not contain references to other weaknesses.
 *
 * @author Jeremias Eppler
 * @see WeaknessCatalog
 */
public class Weakness {
    private final int cweid;
    private final String name;
    private final String description;
    private final WeaknessSeverity severity;

    private Weakness(int cweid, String name, String description, WeaknessSeverity severity) {
        this.cweid = cweid;
        this.name = name;
        this.description = description;
        this.severity = severity;
    }

    /**
     * Creates a new immutable instance of a weakness
     *
     * @param cweid
     * @param name
     * @param description
     * @param severity
     * @return Weakness
     */
    public static Weakness of(@NonNull int cweid, @NonNull String name, @NonNull String description,
            @NonNull WeaknessSeverity severity) {
        return new Weakness(cweid, name, description, severity);
    }

    /**
     * @return the CWE (Common Weakness Enumeration) Id
     */
    public int getCweId() {
        return cweid;
    }

    /**
     * @return the name of the CWE
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description of the CWE id
     */
    public String getDescription() {
        return description;
    }

    /**
     * It returns always a severity (low, high, medium).
     *
     * @return the severity of the CWE id
     */
    public WeaknessSeverity getSeverity() {
        return severity;
    }
}
