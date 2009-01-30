/**
 *
 */
package de.tobject.findbugs.view.properties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenterExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
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

	/**
	 *
	 */
	public BugPatternSection() {
		super();
		titleProvider = new PropPageTitleProvider();
	}

	@Override
	public void createControls(Composite parent,
			final TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
		rootComposite = getWidgetFactory().createFlatFormComposite(parent);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(100, 0);
		Composite p = parent.getParent();
		while(p != null){
			if(p instanceof ScrolledComposite){
				listener = new ControlAdapter() {
					@Override
					public void controlResized(ControlEvent e) {
						if(!rootComposite.isDisposed() && rootComposite.isVisible()) {
							updateDisplay();
						}
					}
				};
				scrolledComposite = (ScrolledComposite) p;
				scrolledComposite.addControlListener(listener);
				break;
			}
			p = p.getParent();
		}

		try {
			browser = new Browser(rootComposite, SWT.NO_BACKGROUND);
			browser.setLayoutData(data);
//			getWidgetFactory().adapt(browser);
		} catch (SWTError e) {
			presentation = new TextPresentation();
			htmlControl = new StyledText(rootComposite, SWT.READ_ONLY | SWT.H_SCROLL
					| SWT.V_SCROLL);
			getWidgetFactory().adapt(htmlControl);
			htmlControl.setLayoutData(data);
			htmlControl.setEditable(false);
			try {
				presenter = new HTMLTextPresenter(false);
			} catch (Exception e2) {
				FindbugsPlugin plugin = FindbugsPlugin.getDefault();
				plugin.logException(new RuntimeException(e.getMessage(), e),
						"Could not create a org.eclipse.swt.widgets.Composite.Browser");
				plugin
						.logException(new RuntimeException(e2.getMessage(), e2),
								"Could not create a org.eclipse.jface.internal.text.html.HTMLTextPresenter");
			}
		}

	}

	protected void updateDisplay() {
		String html;
		if(pattern == null){
			html = "";
		} else {
			html = pattern.getDetailHTML();
			if(html.indexOf("</H1>") > html.indexOf("<H1>")){
				String text = titleProvider.getTitle(pattern);
				int idx1 = html.indexOf("<H1>");
				int idx2 = html.indexOf("</H1>");
				html = html.substring(0, idx1 + 4) + text + html.substring(idx2);
			}
			html = html.replaceAll("H1", "H4");
		}
		Rectangle size = scrolledComposite.getClientArea();
		Composite child = rootComposite;
		Composite p = rootComposite.getParent();
		if(size.width > 0 && size.height > 0){
			while(p != null){
				if(p instanceof ScrolledComposite){
					if(!child.getSize().equals(new Point(size.width, size.height))){
						child.setSize(size.width, size.height);
					}
					break;
				}
				child = p;
				p = p.getParent();
			}
		}

		if (browser != null && !browser.isDisposed()) {
			if(!html.equals(oldText)) {
				browser.setText(html);
			}
		} else {
			StyledText myHtmlControl = htmlControl;
			if (myHtmlControl != null && !myHtmlControl.isDisposed() && presenter != null) {
				html = presenter.updatePresentation(rootComposite.getShell()
						.getDisplay(), html, presentation, size.width,
						size.height);
				myHtmlControl.setText(html);
			}
		}
		oldText = html;
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		pattern = getPattern(selection);
		updateDisplay();
	}

	private BugPattern getPattern(ISelection selection) {
		if(!(selection instanceof IStructuredSelection)){
			return null;
		}
		IStructuredSelection selection2 = (IStructuredSelection) selection;
		Object object = selection2.getFirstElement();
		if(object instanceof BugGroup){
			BugGroup group = (BugGroup) object;
			if(group.getType() == GroupType.Pattern){
				return (BugPattern) group.getData();
			}
		} else if(object instanceof IMarker){
			IMarker marker = (IMarker) object;
			if(MarkerUtil.isFindBugsMarker(marker)) {
				BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
				return bug != null? bug.getBugPattern() : null;
			}
		}
		return null;
	}

	@Override
	public void dispose() {
		if(rootComposite != null && !rootComposite.isDisposed()) {
			scrolledComposite.removeControlListener(listener);
			rootComposite.dispose();
		}
		super.dispose();
	}

	@Override
	public boolean shouldUseExtraSpace() {
		return true;
	}
}
