/**
 * The package providing a {@code BugReporter} implementation which produces report in SARIF format.
 *
 * <table class="plain" style="width:100%">
 *   <caption>Mapping from SARIF concepts to SpotBugs concepts</caption>
 *   <thead>
 *     <tr><th>SARIF</th><th>SpotBugs</th><th>Note</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr><td>ruleId</td><td>BugPattern#type</td><td></td></tr>
 *     <tr><td>results</td><td>BugInstance[]</td><td></td></tr>
 *     <tr><td>shortDescription</td><td>BugPattern#shortDescription</td><td></td></tr>
 *     <tr><td>messageStrings</td><td>BugPattern#longDescription</td><td></td></tr>
 *     <tr><td>category</td><td>BugCategory</td><td></td></tr>
 *     <tr><td>helpUri</td><td>BugPattern#url</td><td></td></tr>
 *     <tr><td>level</td><td>Priorities</td><td></td></tr>
 *     <tr><td>toolExecutionNotification</td><td>AbstractBugReporter#missingClasses<br>AbstractBugReporter#queuedErrors</td><td></td></tr>
 *     <tr><td>reportingDescriptor.helpUri</td><td>BugPattern#url</td><td></td></tr>
 *   </tbody>
 * </table>
 *
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html">Static Analysis Results Interchange Format (SARIF) Version 2.1.0</a>
 */
package edu.umd.cs.findbugs.sarif;
