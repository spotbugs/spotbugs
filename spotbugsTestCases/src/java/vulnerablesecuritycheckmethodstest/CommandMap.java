/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * @(#)CommandMap.java	1.20 07/05/14
 */
package vulnerablesecuritycheckmethodstest;
/**
 * The CommandMap class provides an interface to a registry of
 * command objects available in the system.
 * Developers are expected to either use the CommandMap
 * implementation included with this package (MailcapCommandMap) or
 * develop their own. Note that some of the methods in this class are
 * abstract.
 */
public abstract class CommandMap {
    private static CommandMap defaultCommandMap = null;
    /**
     * Set the default CommandMap. Reset the CommandMap to the default by
     * calling this method with <code>null</code>.
     *
     * @param commandMap The new default CommandMap.
     * @exception SecurityException if the caller doesn't have permission
     *					to change the default
     */
    public static void setDefaultCommandMap(CommandMap commandMap) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            try {
                // if it's ok with the SecurityManager, it's ok with me...
                security.checkSetFactory();
            } catch (SecurityException ex) {
                // otherwise, we also allow it if this code and the
                // factory come from the same class loader (e.g.,
                // the JAF classes were loaded with the applet classes).
                if (CommandMap.class.getClassLoader() !=
                        commandMap.getClass().getClassLoader())
                    throw ex;
            }
        }
        defaultCommandMap = commandMap;
    }
}