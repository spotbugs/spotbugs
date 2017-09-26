/**
 * Implementation of dataflow analysis for checking whether obligations to close streams and other resources
 * (e&#x2E;g&#x2E;, database objects) are satisfied.
 *
 * See <a href="https://doi.org/10.1145/1035292.1029011">Weimer and Necula, Finding and preventing run-time error
 * handling mistakes</a>.
 */
@javax.annotation.ParametersAreNonnullByDefault
@edu.umd.cs.findbugs.internalAnnotations.AnalysisContextContained
package edu.umd.cs.findbugs.ba.obl;
