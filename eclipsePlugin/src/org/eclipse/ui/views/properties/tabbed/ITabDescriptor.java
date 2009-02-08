/**
 * This is a faked interface to avoid compile problems with Eclipse 3.3.
 * Unfortunately in 3.3 internal class TabDescriptor was used in ITabSelectionListener,
 * which was fixed by adding new interface ITabDescriptor in 3.4.
 */
package org.eclipse.ui.views.properties.tabbed;

public interface ITabDescriptor {

	boolean isSelected();

}
