/**
 *
 */
package de.tobject.findbugs.view.properties;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenterExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabDescriptor;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyComposite;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabSelectionListener;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;

/**
 * @author Andrei
 *
 */
public class BugPatternSection extends AbstractPropertySection {

	private Browser browser;
	private StyledText htmlControl;
	private IInformationPresenterExtension presenter;
	private Composite rootComposite;
	private BugPattern pattern;
	private TextPresentation presentation;
	private ScrolledComposite scrolledComposite;
	private ControlAdapter listener;
	private String oldText;
	private final PropPageTitleProvider titleProvider;
	private BugInstance bug;
	private Point scrollSize;
	private boolean inResize;
	private ITabSelectionListener tabSelectionListener;
	private TabbedPropertySheetPage page;
	protected String browserId;
	private volatile boolean allowUrlChange;

	/**
	 *
	 */
	public BugPatternSection() {
		super();
		titleProvider = new PropPageTitleProvider();
	}

	@Override
	public void createControls(Composite parent,
			final TabbedPropertySheetPage page1) {
		super.createControls(parent, page1);
		page = page1;

		createRootComposite(parent);

		initScrolledComposite(parent);

		createBrowser(rootComposite);
	}

	private void createRootComposite(Composite parent) {
		rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginLeft = -5;
		layout.marginTop = -5;
		layout.marginBottom = -5;
		layout.marginRight = -5;
		rootComposite.setLayout(layout);
		rootComposite.setSize(SWT.DEFAULT, SWT.DEFAULT);

		tabSelectionListener = new ITabSelectionListener(){
			/*
			 * interface defined in Eclipse 3.3
			 * remove this crap as soon as we stop supporting Eclipse 3.3
			 */
			@SuppressWarnings({ "restriction" })
			public void tabSelected(TabDescriptor tabDescriptor) {
				if(!rootComposite.isDisposed() && rootComposite.isVisible()
						&& !tabDescriptor.isSelected()) {
					updateBrowserSize();
				}
			}

			/*
			 * interface defined in Eclipse 3.4
			 */
			public void tabSelected(ITabDescriptor tabDescriptor) {
				if(!rootComposite.isDisposed() && rootComposite.isVisible()
						&& !tabDescriptor.isSelected()) {
					updateBrowserSize();
				}
			}
		};
		page.addTabSelectionListener(tabSelectionListener);
	}

	private void createBrowser(Composite parent) {
		Color background = page.getWidgetFactory().getColors().getBackground();
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		try {
			browser = new Browser(parent, SWT.NO_BACKGROUND);
			browser.setLayoutData(data);
			browser.setBackground(background);
			browser.addOpenWindowListener(new OpenWindowListener() {
				public void open(WindowEvent event) {
					event.required = true; // Cancel opening of new windows
				}
			});
			browser.addLocationListener(new LocationListener(){
				public void changed(LocationEvent event) {
					// ignore
				}
				public void changing(LocationEvent event) {
					// fix for SWT code on Won32 platform: it uses "about:blank" before
					// set any non-null url. We ignore this url
					if(allowUrlChange || "about:blank".equals(event.location)){
						return;
					}
					// disallow changing of property view content
					event.doit = false;
					// for any external url clicked by user we should leave property view
					openBrowserInEditor(event);
				}
			});
		} catch (SWTError e) {
			presentation = new TextPresentation();
			htmlControl = new StyledText(parent, SWT.READ_ONLY);
			getWidgetFactory().adapt(htmlControl);
			htmlControl.setLayoutData(data);
			htmlControl.setBackground(background);
			try {
				presenter = new HTMLTextPresenter(false);
			} catch (Exception e2) {
				FindbugsPlugin plugin = FindbugsPlugin.getDefault();
				plugin.logException(new RuntimeException(e.getMessage(), e),
						"Could not create org.eclipse.swt.widgets.Composite.Browser");
				plugin
						.logException(new RuntimeException(e2.getMessage(), e2),
								"Could not create org.eclipse.jface.internal.text.html.HTMLTextPresenter");
			}
		}
	}

	private void initScrolledComposite(Composite parent) {
		scrolledComposite = ((TabbedPropertyComposite)page.getControl()).getScrolledComposite();
		// same as above but without warning:

//		Composite p = parent.getParent();
//		while(p != null){
//			if(p instanceof ScrolledComposite){
//				scrolledComposite = (ScrolledComposite) p;
//				break;
//			}
//			p = p.getParent();
//		}

		if(scrolledComposite != null){
			listener = new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					if(!rootComposite.isDisposed() && rootComposite.isVisible()) {
						updateBrowserSize();
					}
				}
			};
			scrolledComposite.addControlListener(listener);
		}
	}

	protected void updateDisplay() {
		String html = null;
		if (browser != null && !browser.isDisposed()) {
			html = getHtml();
			// required even if html is the same: our client area might be changed
			updateBrowserSize();
			// avoid flickering if same input
			if(!html.equals(oldText)) {
				allowUrlChange = true;
				browser.setText(html);
				allowUrlChange = false;
			}
		} else {
			if (htmlControl != null && !htmlControl.isDisposed() && presenter != null) {
				Rectangle clientArea = updateBrowserSize();
				htmlControl.setSize(clientArea.width - 5, clientArea.height - 5);
				html = getHtml();
				try {
					html = presenter.updatePresentation(rootComposite.getShell()
						.getDisplay(), html, presentation, clientArea.width, clientArea.height);
				} catch (StringIndexOutOfBoundsException e) {
					// I can't understand why it happens, but it happens...
				}
				htmlControl.setText(html);
			}
		}
		oldText = html;
	}

	/**
	 * Updates the browser/scrolledComposite size to avoid second pair of scrollbars
	 */
	private Rectangle updateBrowserSize() {
		Point newScrollSize = scrolledComposite.getSize();
		Rectangle clientArea = scrolledComposite.getClientArea();
		Point rootSize = rootComposite.getSize();
		if (!inResize && clientArea.width > 0 && clientArea.height > 0
				&& (!newScrollSize.equals(scrollSize)
						|| clientArea.width != rootSize.x || clientArea.height != rootSize.y)) {

			scrollSize = newScrollSize;
			inResize = true;
			rootComposite.setSize(clientArea.width, clientArea.height);
			scrolledComposite.setMinSize(clientArea.width, clientArea.height);
			scrolledComposite.layout();
			inResize = false;
		}
		return clientArea;
	}

	private String getHtml() {
		if(pattern == null){
			return "";
		}
		boolean hasBug = bug != null;
		StringBuilder text = new StringBuilder();
		if(!hasBug){
			text.append("<b>Pattern:</b> ");
			text.append(pattern.getShortDescription());
			text.append("<br>");
		} else {
			text.append("<b>Pattern</b> ");
		}
		text.append(titleProvider.getDetails(pattern));
		text.append("<br><br>");
		text.append(pattern.getDetailText());
		String html = text.toString();
		if (hasBug) {
			html = "<b>Bug:</b> " + toSafeHtml(bug.getAbridgedMessage()) + "<br>\n" + html;
		}
		return html;
	}

	private String toSafeHtml(String s) {
		if(s.indexOf(">") >= 0){
			s = s.replace(">", "&gt;");
		}
		if(s.indexOf("<") >= 0){
			s = s.replace("<", "&lt;");
		}
		return s;
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		boolean contentChanged = contentChanged(selection);
		if(contentChanged) {
			updateDisplay();
		}
	}

	/**
	 * Updates pattern and bug from selection
	 * @return true if the content is changed
	 */
	private boolean contentChanged(ISelection selection) {
		boolean existsBefore = pattern != null;
		if(!(selection instanceof IStructuredSelection)){
			bug = null;
			pattern = null;
			return existsBefore;
		}
		IStructuredSelection selection2 = (IStructuredSelection) selection;
		Object object = selection2.getFirstElement();
		if(object instanceof BugGroup){
			BugGroup group = (BugGroup) object;
			if(group.getType() == GroupType.Pattern){
				bug = null;
				BugPattern data = (BugPattern) group.getData();
				BugPattern old = pattern;
				pattern = data;
				return old != data;
			}
		} else if(object instanceof IMarker){
			IMarker marker = (IMarker) object;
			if(MarkerUtil.isFindBugsMarker(marker)) {
				BugInstance bugInstance = MarkerUtil.findBugInstanceForMarker(marker);
				BugInstance old = bug;
				bug = bugInstance;
				pattern = bug != null ? bug.getBugPattern() : null;
				return  old != bugInstance;
			}
		}
		return existsBefore;
	}

	@Override
	public void dispose() {
		if(rootComposite != null && !rootComposite.isDisposed()) {
			page.removeTabSelectionListener(tabSelectionListener);
			scrolledComposite.removeControlListener(listener);
			rootComposite.dispose();
		}
		super.dispose();
	}

	@Override
	public boolean shouldUseExtraSpace() {
		return true;
	}

	private void openBrowserInEditor(LocationEvent event) {
		URL url;
		try {
			url = new URL(event.location);
		} catch (MalformedURLException e) {
			return;
		}
		IWorkbenchBrowserSupport support= PlatformUI.getWorkbench().getBrowserSupport();
		try {
			IWebBrowser newBrowser= support.createBrowser(browserId);
			browserId = newBrowser.getId();
			newBrowser.openURL(url);
			return;
		} catch (PartInitException e) {
			FindbugsPlugin.getDefault().logException(e, "Can't open external browser");
		}
	}
}
