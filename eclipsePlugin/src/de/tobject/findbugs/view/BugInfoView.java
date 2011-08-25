
package de.tobject.findbugs.view;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenterExtension;
import org.eclipse.jface.text.TextPresentation;
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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.EditorUtil;
import de.tobject.findbugs.util.Util;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugAnnotationWithSourceLines;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
* @author Andrei
*
*/
public class BugInfoView extends AbstractFindbugsView {

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

   protected String browserId;

   private volatile boolean allowUrlChange;

   protected List annotationList;

   private IMarker marker;

   private IFile file;

   private String title;

   private IJavaElement javaElt;

   private FormToolkit toolkit;

   private ISelectionListener selectionListener;



   /**
    *
    */
   public BugInfoView() {
       super();
       titleProvider = new PropPageTitleProvider();
   }

   @Override
   public Composite createRootControl(Composite parent) {

       toolkit = new FormToolkit(parent.getDisplay());
       createRootComposite(parent);


       makeAnnotationList(rootComposite);
//        initScrolledComposite(parent);
       createBrowser(rootComposite);
       // Add selection listener to detect click in problems view or bug tree
       // view
       ISelectionService theService = getSite().getWorkbenchWindow().getSelectionService();

       selectionListener = new MarkerSelectionListener(this);
       theService.addSelectionListener(selectionListener);

       return rootComposite;
   }

   private void createRootComposite(Composite parent) {
       rootComposite = new Composite(parent, SWT.NONE);
       GridLayout layout = new GridLayout(1, true);
       layout.verticalSpacing = 10;
       rootComposite.setLayout(layout);
       rootComposite.setSize(SWT.DEFAULT, SWT.DEFAULT);
       Color background = toolkit.getColors().getBackground();
       rootComposite.setBackground(background);
       }

   private void createBrowser(Composite parent) {
       Color background = toolkit.getColors().getBackground();
       GridData data = new GridData(GridData.FILL_BOTH);
       data.horizontalIndent = 0;
       data.verticalIndent = 0;
       try {
           browser = new Browser(parent, SWT.NO_BACKGROUND);
           if (parent instanceof ScrolledComposite) {
            browser.setLayout(new FillLayout());
        } else {
            browser.setLayoutData(data);
        }
           browser.setBackground(background);
           browser.addOpenWindowListener(new OpenWindowListener() {
               public void open(WindowEvent event) {
                   event.required = true; // Cancel opening of new windows
               }
           });
           browser.addLocationListener(new LocationListener() {
               public void changed(LocationEvent event) {
                   // ignore
               }

               public void changing(LocationEvent event) {
                   // fix for SWT code on Won32 platform: it uses "about:blank"
                   // before
                   // set any non-null url. We ignore this url
                   if (allowUrlChange || "about:blank".equals(event.location)) {
                       return;
                   }
                   // disallow changing of property view content
                   event.doit = false;
                   // for any external url clicked by user we should leave
                   // property view
                   openBrowserInEditor(event);
               }
           });
       } catch (SWTError e) {
           presentation = new TextPresentation();
           htmlControl = new StyledText(parent, SWT.READ_ONLY);
           toolkit.adapt(htmlControl);
           if (parent instanceof ScrolledComposite) {
            htmlControl.setLayout(new FillLayout());
        } else {
            htmlControl.setLayoutData(data);
        }

           htmlControl.setBackground(background);
           try {
               presenter = new HTMLTextPresenter(false);
           } catch (Exception e2) {
               FindbugsPlugin plugin = FindbugsPlugin.getDefault();
               plugin.logException(new RuntimeException(e.getMessage(), e),
                       "Could not create org.eclipse.swt.widgets.Composite.Browser");
               plugin.logException(new RuntimeException(e2.getMessage(), e2),
                       "Could not create org.eclipse.jface.internal.text.html.HTMLTextPresenter");
           }
       }
   }

   private  void makeAnnotationList(Composite panel) {
       annotationList = new List(panel, SWT.NONE);
       GridData data = new GridData(GridData.FILL_HORIZONTAL);
       data.horizontalIndent = 0;
       data.verticalIndent = 0;
       annotationList.setLayoutData(data);

       annotationList.setFont(JFaceResources.getDialogFont());
       annotationList.addSelectionListener(new SelectionAdapter() {
           @Override
           public void widgetSelected(SelectionEvent evnt) {
               selectInEditor(false);
           }
       });
       annotationList.addMouseListener(new MouseAdapter() {
           @Override
           public void mouseDoubleClick(MouseEvent e) {
               selectInEditor(true);
           }
       });
       final Menu menu = new Menu(annotationList);
       final MenuItem item = new MenuItem(menu, SWT.PUSH);
       item.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
       item.setText("Copy To Clipboard");
       item.addListener(SWT.Selection, new Listener() {
           public void handleEvent(Event e) {
               copyInfoToClipboard();
           }
       });
       menu.addListener(SWT.Show, new Listener() {
           public void handleEvent(Event event) {
               item.setEnabled(bug != null);
           }
       });
       annotationList.setMenu(menu);
       annotationList.pack(true);

   }



    private void initScrolledComposite(Composite parent) {
        scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);

        listener = new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (!rootComposite.isDisposed() && rootComposite.isVisible()) {
                    updateBrowserSize();
                }
            }
        };
        scrolledComposite.addControlListener(listener);

    }

   protected void updateDisplay() {
       String html = null;
       if (browser != null && !browser.isDisposed()) {
           html = getHtml();
           // required even if html is the same: our client area might be
           // changed
           updateBrowserSize();
           // avoid flickering if same input
           if (!html.equals(oldText)) {
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
                   html = presenter.updatePresentation(rootComposite.getShell().getDisplay(), html, presentation,
                           clientArea.width, clientArea.height);
               } catch (StringIndexOutOfBoundsException e) {
                   // I can't understand why it happens, but it happens...
               }
               htmlControl.setText(html);
           }
       }
       oldText = html;
   }

   /**
    * Updates the browser/scrolledComposite size to avoid second pair of
    * scrollbars
    */
   private Rectangle updateBrowserSize() {
       if (scrolledComposite == null ) {
        return rootComposite.getClientArea();
    }
       Point newScrollSize = scrolledComposite.getSize();
       Rectangle clientArea = scrolledComposite.getClientArea();
       if (!inResize && clientArea.width > 0 && clientArea.height > 0
               && !newScrollSize.equals(scrollSize)) {

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
       if (pattern == null) {
           return "";
       }
       boolean hasBug = bug != null;
       StringBuilder text = new StringBuilder();
       text.append(pattern.getDetailText());
       if (!hasBug) {
           return text.toString();
       }
       DetectorFactory factory = bug.getDetectorFactory();
       if (factory != null) {
           Plugin plugin = factory.getPlugin();
           if (!plugin.isCorePlugin()) {
           text.append("<p><small><i>Reported by: ").append(factory.getFullName());
           text.append("<br>Contributed by plugin: ").append(plugin.getPluginId());
           text.append("<br>Provider: ").append(plugin.getProvider());
           String website = plugin.getWebsite();
           if (website != null && website.length() > 0) {
               text.append(" (<a href=\"").append(website).append("\">");
               text.append(website).append("</a>)");
           }
           text.append("</i></small>");
           }
       }
       String html = text.toString();
       return html;
   }

   private String toSafeHtml(String s) {
       if (s.indexOf(">") >= 0) {
           s = s.replace(">", "&gt;");
       }
       if (s.indexOf("<") >= 0) {
           s = s.replace("<", "&lt;");
       }
       return s;
   }

   /**
    * Updates pattern and bug from selection
    *
    * @return true if the content is changed
    */
   private boolean contentChanged(ISelection selection) {
       boolean existsBefore = pattern != null;
       if (!(selection instanceof IStructuredSelection)) {
           bug = null;
           pattern = null;
           return existsBefore;
       }
       IStructuredSelection selection2 = (IStructuredSelection) selection;
       Object object = selection2.getFirstElement();
       if (object instanceof BugGroup) {
           BugGroup group = (BugGroup) object;
           marker = null;
           if (group.getType() == GroupType.Pattern) {
               bug = null;
               BugPattern data = (BugPattern) group.getData();
               BugPattern old = pattern;
               pattern = data;
               refreshTitle();
               refreshAnnotations();
               updateDisplay();
               return old != data;
           }
       } else if (object instanceof IMarker) {
           showInView((IMarker) object);

       }
       return existsBefore;
   }

   @Override
   public void dispose() {
       if (selectionListener != null) {
           getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
           selectionListener = null;
       }
       if (rootComposite != null && !rootComposite.isDisposed()) {
           scrolledComposite.removeControlListener(listener);
           rootComposite.dispose();
       }
       super.dispose();
   }


   private void openBrowserInEditor(LocationEvent event) {
       URL url;
       try {
           url = new URL(event.location);
       } catch (MalformedURLException e) {
           return;
       }
       IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
       try {
           IWebBrowser newBrowser = support.createBrowser(browserId);
           browserId = newBrowser.getId();
           newBrowser.openURL(url);
           return;
       } catch (PartInitException e) {
           FindbugsPlugin.getDefault().logException(e, "Can't open external browser");
       }
   }

   private void refreshAnnotations() {
       annotationList.removeAll();
       // bug may be null, but if so then the error has already been logged.
       if (bug != null) {
           for(BugAnnotation ba : bug.getAnnotationsForMessage(false)) {
               annotationList.add(ba.toString());
           }
       }
       annotationList.pack(true);
   }

    protected int getLineToSelect() {
        int index = annotationList.getSelectionIndex();
        BugAnnotation theAnnotation = bug.getAnnotationsForMessage(false).get(index);
        System.out.println("Selected item " + index + " : " + theAnnotation);
        SourceLineAnnotation sla;
        if (theAnnotation instanceof SourceLineAnnotation) {
            sla = (SourceLineAnnotation) theAnnotation;
        } else if (theAnnotation instanceof BugAnnotationWithSourceLines) {
            sla = ((BugAnnotationWithSourceLines) theAnnotation).getSourceLines();
        } else {
            return marker.getAttribute(IMarker.LINE_NUMBER, EditorUtil.DEFAULT_LINE_IN_EDITOR);
        }
        int startLine = sla.getStartLine();
        return startLine <= 0 ? EditorUtil.DEFAULT_LINE_IN_EDITOR : startLine;
    }

   protected void copyInfoToClipboard() {
       StringBuffer sb = new StringBuffer();
       sb.append(title);
       sb.append("\n");
       for(BugAnnotation ba : bug.getAnnotationsForMessage(true)) {
          sb.append(ba.toString()).append("\n");
       }
       if (file != null) {
           sb.append(file.getLocation()).append("\n");
       }
       Util.copyToClipboard(sb.toString());
   }

   private void selectInEditor(boolean openEditor) {
       if (bug == null || (file == null && javaElt == null)) {
           return;
       }
       IWorkbenchPage page = contributingPart.getSite().getPage();
       IEditorPart activeEditor = page.getActiveEditor();
       IEditorInput input = activeEditor != null ? activeEditor.getEditorInput() : null;

       if (openEditor && !matchInput(input)) {
           try {
               if (file != null) {
                   activeEditor = IDE.openEditor(page, file);
               } else if (javaElt != null) {
                   activeEditor = JavaUI.openInEditor(javaElt, true, true);
               }
               if (activeEditor != null) {
                   input = activeEditor.getEditorInput();
               }
           } catch (PartInitException e) {
               FindbugsPlugin.getDefault().logException(e, "Could not open editor for " + bug.getMessage());
           } catch (CoreException e) {
               FindbugsPlugin.getDefault().logException(e, "Could not open editor for " + bug.getMessage());
           }
       }
       if (matchInput(input)) {
           int startLine = getLineToSelect();
           EditorUtil.goToLine(activeEditor, startLine);
       }
   }

   private boolean matchInput(IEditorInput input) {
       if (file != null && (input instanceof IFileEditorInput)) {
           return file.equals(((IFileEditorInput) input).getFile());
       }
       if (javaElt != null && input != null) {
           IJavaElement javaElement = JavaUI.getEditorInputJavaElement(input);
           if (javaElt.equals(javaElement)) {
               return true;
           }
           IJavaElement parent = javaElt.getParent();
           while (parent != null && !parent.equals(javaElement)) {
               parent = parent.getParent();
           }
           if (parent != null && parent.equals(javaElement)) {
               return true;
           }
       }
       return false;
   }

    private void refreshTitle() {
        BugPattern pattern = null;

        if (marker != null) {
            String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
            pattern = DetectorFactoryCollection.instance().lookupBugPattern(bugType);
        } else if (this.pattern != null) {
            pattern = this.pattern;
        }
        if (pattern == null) {
            title = "";
            return;
        }
        if (bug == null) {
            title = pattern.getShortDescription();
            return;
        }
        String shortDescription = bug.getAbridgedMessage();
        String abbrev = "[" + bug.getPriorityAbbreviation() + " " + bug.getCategoryAbbrev() + " " + pattern.getAbbrev() + "] ";
        if (shortDescription == null) {
            title = abbrev;
        } else {
            title = abbrev + shortDescription.trim() + " [" + pattern.getType() + "]";
        }

    }

    private IWorkbenchPart contributingPart;

    @Override
    public void markerSelected(IWorkbenchPart thePart, IMarker newMarker) {
        contributingPart = thePart;
        showInView(newMarker);
        if (!isVisible()) {
            activate();
        }
    }

    public IWorkbenchPart getContributingPart() {
        return contributingPart;
    }

    private void showInView(IMarker m) {
        this.marker = m;
        if (MarkerUtil.isFindBugsMarker(marker)) {
            BugInstance bugInstance = MarkerUtil.findBugInstanceForMarker(marker);
            bug = bugInstance;
            file = (IFile) (marker.getResource() instanceof IFile ? marker.getResource() : null);
            javaElt = MarkerUtil.findJavaElementForMarker(marker);

            pattern = bug != null ? bug.getBugPattern() : null;
            refreshTitle();
            refreshAnnotations();
            updateDisplay();
        }
    }


}
