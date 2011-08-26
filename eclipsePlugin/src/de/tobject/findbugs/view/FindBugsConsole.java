/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
package de.tobject.findbugs.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.themes.ITheme;

/**
 * @author Andrei
 */
public class FindBugsConsole extends MessageConsole implements IPropertyChangeListener {
    private static final String CONSOLE_FONT = "findBugsEclipsePlugin.consoleFont";
    static FindBugsConsole console;

    boolean disposed;

    private static class RemoveAction extends Action {
        public RemoveAction() {
            super("Close FindBugs console", PlatformUI.getWorkbench().getSharedImages()
                    .getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        }

        @Override
        public void run() {
            IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
            if (console != null) {
                manager.removeConsoles(new IConsole[] { console });
                console = null;
            }
        }
    }

    private FindBugsConsole(String name, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
        super(name, imageDescriptor, autoLifecycle);

    }

    public void propertyChange(PropertyChangeEvent event) {
        if (CONSOLE_FONT.equals(event.getProperty())) {
            setConsoleFont();
        }
    }

    @Override
    protected void dispose() {
        if (!disposed) {
            disposed = true;
            ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            theme.removePropertyChangeListener(this);
            super.dispose();
        }
    }

    private void setConsoleFont() {
        if (Display.getCurrent() == null) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    setConsoleFont();
                }
            });
        } else {
            ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            Font font = theme.getFontRegistry().get(CONSOLE_FONT);
            console.setFont(font);
        }
    }

    public static class FindBugsConsoleFactory implements IConsoleFactory {

        public void openConsole() {
            showConsole();
        }

    }

    public static FindBugsConsole showConsole() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        boolean exists = false;
        if (console != null) {
            IConsole[] existing = manager.getConsoles();
            for (int i = 0; i < existing.length; i++) {
                if (console == existing[i]) {
                    exists = true;
                }
            }
        } else {
            console = new FindBugsConsole("FindBugs", null, true);
        }
        if (!exists) {
            manager.addConsoles(new IConsole[] { console });
        }
        ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        theme.addPropertyChangeListener(console);
        console.setConsoleFont();
        manager.showConsoleView(console);
        return console;
    }

    public static MessageConsole getConsole() {
        return console;
    }

    public static class FindBugsConsolePageParticipant implements IConsolePageParticipant {

        private RemoveAction removeAction;

        public void activated() {
            // noop
        }

        public void deactivated() {
            // noop
        }

        public void dispose() {
            removeAction = null;
            // followed causes sometimes problems with console removal
            // if (console != null) {
            // console.dispose();
            // console = null;
            // }
        }

        public void init(IPageBookViewPage page, IConsole console1) {
            removeAction = new RemoveAction();
            IActionBars bars = page.getSite().getActionBars();
            bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeAction);
        }

        public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
            return null;
        }
    }
}
