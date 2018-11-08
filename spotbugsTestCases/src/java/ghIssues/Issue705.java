/*
 * Contributions to SpotBugs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ghIssues;

public class Issue705 {
    
    public static String bug() {
        String replaceable = "replaceable";
        String replacement = "replacement";
        String target = "target";
        return target.replaceAll(replaceable, replacement);
    }

    public static String nonBug() {
        String replaceable = "(replaceable)";
        String replacement = "replacement";
        String target = "target";
        return target.replaceAll(replaceable, replacement);
    }
    
    public static String nonBug2() {
        String replacement = "replacement";
        String target = "target";
        target = target.replaceAll(".", replacement);
        target = target.replaceAll("\\", replacement);
        target = target.replaceAll("[", replacement);
        target = target.replaceAll("]", replacement);
        target = target.replaceAll("{", replacement);
        target = target.replaceAll("}", replacement);
        target = target.replaceAll("(", replacement);
        target = target.replaceAll(")", replacement);
        target = target.replaceAll("<", replacement);
        target = target.replaceAll(">", replacement);
        target = target.replaceAll("*", replacement);
        target = target.replaceAll("+", replacement);
        target = target.replaceAll("-", replacement);
        target = target.replaceAll("=", replacement);
        target = target.replaceAll("?", replacement);
        target = target.replaceAll("^", replacement);
        target = target.replaceAll("$", replacement);
        target = target.replaceAll("|", replacement);
        return target;
    }
    
    public static String nonBugNull() {
        String replaceable = null;
        String replacement = "replacement";
        String target = "target";
        return target.replaceAll(replaceable, replacement);
    }
}
