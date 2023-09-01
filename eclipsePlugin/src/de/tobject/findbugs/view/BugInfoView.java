/*
 * Contributions to FindBugs
 * Copyright (C) 2011, Andrey Loskutov
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

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

import de.tobject.findbugs.util.SafeHtml;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.ide.IDE;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.marker.FindBugsMarker.MarkerConfidence;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.EditorUtil;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRankCategory;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * @author Andrei
 *
 */
public class BugInfoView extends AbstractFindbugsView {

    private Browser browser;

    private Composite rootComposite;

    private BugPattern pattern;


    private String oldText;

    private BugInstance bug;

    private String browserId;

    private volatile boolean allowUrlChange;

    private List annotationList;

    private IMarker marker;

    private IFile file;

    private IJavaElement javaElt;

    private ISelectionListener selectionListener;

    private IWorkbenchPart contributingPart;

    private volatile boolean showingAnnotation;

    private final IExpansionListener expansionListener;

    public BugInfoView() {
        super();
        expansionListener = new IExpansionListener() {
            @Override
            public void expansionStateChanging(ExpansionEvent e) {
                // noop
            }

            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                rootComposite.layout(true, true);
                rootComposite.redraw();
            }
        };
    }

    @Override
    public Composite createRootControl(Composite parent) {

        createRootComposite(parent);

        createAnnotationList(rootComposite);
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
        layout.marginLeft = -5;
        layout.marginTop = -5;
        layout.marginBottom = -5;
        layout.marginRight = -5;
        rootComposite.setLayout(layout);
        rootComposite.setSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    private void createBrowser(Composite parent) {
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        try {
            browser = new Browser(parent, SWT.NONE);
            browser.setLayoutData(data);
            browser.setBackground(parent.getBackground());
            browser.addOpenWindowListener(new OpenWindowListener() {
                @Override
                public void open(WindowEvent event) {
                    event.required = true; // Cancel opening of new windows
                }
            });
            browser.addLocationListener(new LocationListener() {
                @Override
                public void changed(LocationEvent event) {
                    // ignore
                }

                @Override
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
            FindbugsPlugin plugin = FindbugsPlugin.getDefault();
            plugin.logException(new RuntimeException(e.getMessage(), e),
                    "Could not create org.eclipse.swt.widgets.Composite.Browser");

        }
    }

    private void createAnnotationList(Composite parent) {
        ExpandableComposite exp = new ExpandableComposite(parent, SWT.NONE,
                ExpandableComposite.TREE_NODE
                        | ExpandableComposite.COMPACT
                        | ExpandableComposite.EXPANDED
        //                | ExpandableComposite.NO_TITLE
        //                | ExpandableComposite.FOCUS_TITLE
        //                | ExpandableComposite.TITLE_BAR
        //                | ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT
        //| ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT
        );
        exp.addExpansionListener(expansionListener);
        exp.setText("Navigation");
        annotationList = new List(exp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);

        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        exp.setLayoutData(data);
        exp.setClient(annotationList);
        exp.setBackground(parent.getBackground());
        exp.setFont(JFaceResources.getDialogFont());
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
            @Override
            public void handleEvent(Event e) {
                copyInfoToClipboard();
            }
        });
        menu.addListener(SWT.Show, new Listener() {
            @Override
            public void handleEvent(Event event) {
                item.setEnabled(bug != null);
            }
        });
        annotationList.setToolTipText("Click on lines or methods to go to them");
        annotationList.setMenu(menu);
        annotationList.pack(true);
    }

    private void refreshBrowser() {
        String html = null;
        if (browser != null && !browser.isDisposed()) {
            html = getHtml();

            // avoid flickering if same input
            if (!html.equals(oldText)) {
                allowUrlChange = true;
                browser.setText(html);
                allowUrlChange = false;
            }
        }
        oldText = html;
    }


    private String getHtml() {
        if (pattern == null) {
            return "";
        }
        boolean hasBug = bug != null;
        StringBuilder text = new StringBuilder();
        if (!hasBug) {
            text.append("<b>Pattern</b>: ");
            text.append(pattern.getShortDescription());
        } else {
            text.append(pattern.getDetailText());
        }
        if (!hasBug) {
            return text.toString();
        }
        if (text.lastIndexOf("</p>") == -1 || text.lastIndexOf("<br>") == -1) {
            text.append("\n<p>");
        }
        text.append(getBugDetails());
        text.append("<br>");
        text.append(getPatternDetails());
        addXmlOutput(text);
        addDetectorInfo(text);
        String html = "<b>Bug</b>: " + SafeHtml.escape(bug.getMessageWithoutPrefix()) + "<br>\n" + text.toString();
        return html;
    }

    private void addXmlOutput(StringBuilder text) {
        StringWriter stringWriter = new StringWriter();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(stringWriter);
        try {
            bug.writeXML(xmlOutput);
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                xmlOutput.finish();
            } catch (IOException e) {
                // ignore
            }
        }
        text.append("<hr size=\"1\" /><p><b>XML output:</b>");
        text.append("<pre>");
        text.append(SafeHtml.escape(stringWriter.toString()));
        text.append("</pre></p><hr size=\"1\" />");
    }

    private String getBugDetails() {
        StringBuilder sb = new StringBuilder();
        int rank = 0;
        MarkerConfidence confidence = MarkerConfidence.Ignore;
        if (bug != null) {
            confidence = MarkerConfidence.getConfidence(bug.getPriority());
            rank = bug.getBugRank();
        } else if (marker != null) {
            confidence = MarkerUtil.findConfidenceForMarker(marker);
            rank = MarkerUtil.findBugRankForMarker(marker);
        }
        sb.append("\n<b>Rank</b>: ");
        sb.append(BugRankCategory.getRank(rank));
        sb.append(" (").append(rank).append(")");
        sb.append(", <b>confidence</b>: ").append(confidence);
        return sb.toString();
    }

    private String getPatternDetails() {
        if (pattern == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<b>Pattern</b>: ");
        sb.append(pattern.getType());
        sb.append("\n<br><b>Type</b>: ").append(pattern.getAbbrev()).append(", <b>Category</b>: ");
        sb.append(pattern.getCategory());
        BugCategory category = DetectorFactoryCollection.instance().getBugCategory(pattern.getCategory());
        if (category != null) {
            sb.append(" (");
            sb.append(category.getShortDescription());
            sb.append(")");
        }
        return sb.toString();
    }

    private void addDetectorInfo(StringBuilder text) {
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
    }

    @Override
    public void dispose() {
        if (selectionListener != null) {
            getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
            selectionListener = null;
        }
        if (rootComposite != null && !rootComposite.isDisposed()) {
            rootComposite.dispose();
        }
        super.dispose();
    }

    private void openBrowserInEditor(LocationEvent event) {
        URL url;
        try {
            url = new URL(event.location);
        } catch (MalformedURLException ignored) {
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
            annotationList.add(bug.getMessageWithoutPrefix());
            for (BugAnnotation ba : bug.getAnnotationsForMessage(false)) {
                annotationList.add(ba.toString());
            }
        }
        annotationList.pack(true);
    }

    private IJavaProject getIProject() {
        if (javaElt != null) {
            return javaElt.getJavaProject();
        }

        if (file != null) {
            IProject p = file.getProject();
            try {
                if (p.hasNature(JavaCore.NATURE_ID)) {
                    return JavaCore.create(p);
                }
            } catch (CoreException e) {
                FindbugsPlugin.getDefault().logException(e, "Could not open Java project for " + file);
            }
        }
        return null;
    }


    private void showAnnotation(IEditorPart activeEditor) {
        if (showingAnnotation) {
            FindbugsPlugin.getDefault().logInfo("Recursive showAnnotation");
        }
        showingAnnotation = true;

        try {

            int index = annotationList.getSelectionIndex() - 1;
            if (index >= 0) {
                BugAnnotation theAnnotation = bug.getAnnotationsForMessage(false).get(index);
                findLocation: try {

                    if (theAnnotation instanceof SourceLineAnnotation) {
                        SourceLineAnnotation sla = (SourceLineAnnotation) theAnnotation;
                        int line = sla.getStartLine();
                        EditorUtil.goToLine(activeEditor, line);
                        return;

                    } else if (theAnnotation instanceof MethodAnnotation) {
                        MethodAnnotation ma = (MethodAnnotation) theAnnotation;
                        String className = ma.getClassName();
                        IJavaProject project = getIProject();
                        IType type = project.findType(className);
                        if (type == null) {
                            break findLocation;
                        }
                        IMethod m = getIMethod(type, ma);
                        if (m != null) {
                            JavaUI.openInEditor(m, true, true);

                        } else {
                            activeEditor = JavaUI.openInEditor(type, true, true);
                            SourceLineAnnotation sla = ma.getSourceLines();
                            EditorUtil.goToLine(activeEditor, sla.getStartLine());
                        }

                        return;

                    } else if (theAnnotation instanceof FieldAnnotation) {
                        FieldAnnotation fa = (FieldAnnotation) theAnnotation;
                        String className = fa.getClassName();
                        IJavaProject project = getIProject();
                        IType type = project.findType(className);
                        if (type == null) {
                            break findLocation;
                        }

                        IField f = type.getField(fa.getFieldName());
                        if (f != null) {
                            JavaUI.openInEditor(f, true, true);
                        } else {
                            activeEditor = JavaUI.openInEditor(type, true, true);
                            SourceLineAnnotation sla = fa.getSourceLines();
                            EditorUtil.goToLine(activeEditor, sla.getStartLine());
                        }
                        return;
                    } else if (theAnnotation instanceof TypeAnnotation) {
                        TypeAnnotation fa = (TypeAnnotation) theAnnotation;
                        String className = ClassName.fromFieldSignature(fa.getTypeDescriptor());
                        if (className == null) {
                            break findLocation;
                        }
                        IJavaProject project = getIProject();
                        IType type = project.findType(ClassName.toDottedClassName(className));
                        if (type == null) {
                            break findLocation;
                        }
                        JavaUI.openInEditor(type, true, true);
                        return;

                    } else if (theAnnotation instanceof ClassAnnotation) {
                        ClassAnnotation fa = (ClassAnnotation) theAnnotation;
                        String className = fa.getClassName();
                        IJavaProject project = getIProject();
                        IType type = project.findType(className);
                        if (type == null) {
                            break findLocation;
                        }
                        JavaUI.openInEditor(type, true, true);
                        return;
                    }
                } catch (JavaModelException e) {
                    FindbugsPlugin.getDefault().logException(e, "Could not open editor for " + theAnnotation);
                } catch (PartInitException e) {
                    FindbugsPlugin.getDefault().logException(e, "Could not open editor for " + theAnnotation);
                }
            }
            if (marker != null) {
                int line = marker.getAttribute(IMarker.LINE_NUMBER, EditorUtil.DEFAULT_LINE_IN_EDITOR);
                EditorUtil.goToLine(activeEditor, line);
            }
        } finally {
            showingAnnotation = false;
        }
    }

    private static String stripFirstAndLast(String s) {
        return s.substring(1, s.length() - 1);
    }

    private static IMethod getIMethod(IType type, MethodAnnotation mma) throws JavaModelException {
        String name = mma.getMethodName();
        SignatureParser parser = new SignatureParser(mma.getMethodSignature());
        String[] arguments = parser.getArguments();


        nextMethod: for (IMethod m : type.getMethods()) {
            if (!m.getElementName().equals(name)) {
                continue nextMethod;
            }

            String[] mArguments = m.getParameterTypes();
            if (arguments.length != mArguments.length) {
                continue nextMethod;
            }

            for (int i = 0; i < arguments.length; i++) {
                String a = arguments[i];
                String ma = mArguments[i];
                while (a.startsWith("[") && ma.startsWith("[")) {
                    a = a.substring(1);
                    ma = ma.substring(1);
                }
                if (ma.startsWith("Q")) {
                    ma = stripFirstAndLast(ma);
                    ClassDescriptor ad = DescriptorFactory.createClassDescriptorFromFieldSignature(a);
                    if (ad == null) {
                        continue nextMethod;
                    }
                    a = ad.getSimpleName();
                }
                if (!ma.equals(a)) {
                    continue nextMethod;
                }
            }
            return m;
        }
        return null;
    }

    private void copyInfoToClipboard() {
        if (bug == null) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        sb.append(removeHtmlMarkup(getHtml()));
        sb.append("\n\n");
        for (BugAnnotation ba : bug.getAnnotationsForMessage(true)) {
            sb.append(ba.toString()).append("\n");
        }
        sb.append("\n");
        if (file != null) {
            sb.append("File: ").append(file.getLocation()).append("\n");
        }
        Util.copyToClipboard(sb.toString());
    }

    private static String removeHtmlMarkup(String html) {
        // replace any amount of white space with newline between through one
        // space
        html = html.replaceAll("\\s*[\\n]+\\s*", " ");
        // remove all valid html tags
        html = html.replaceAll("<[a-zA-Z]+>", "\n");
        html = html.replaceAll("</[a-zA-Z]+>", "");
        // convert some of the entities which are used in current FB
        // messages.xml
        html = html.replaceAll("&nbsp;", "");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        html = html.replaceAll("&amp;", "&");
        return html.trim();
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
            showAnnotation(activeEditor);

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
        if (marker != null) {
            String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
            pattern = DetectorFactoryCollection.instance().lookupBugPattern(bugType);
        }
        if (pattern == null) {
            return;
        }
        if (bug == null) {
            return;
        }
        if (file != null) {
            setContentDescription(file.getName() +
                    ": " + marker.getAttribute(IMarker.LINE_NUMBER, 0));
        } else {
            setContentDescription("");
        }
    }

    @Override
    public void markerSelected(IWorkbenchPart thePart, IMarker newMarker) {
        if (showingAnnotation) {
            return;
        }
        contributingPart = thePart;
        showInView(newMarker);
        if (!isVisible()) {
            activate();
        }
    }

    @Override
    public IWorkbenchPart getContributingPart() {
        return contributingPart;
    }

    private void showInView(IMarker m) {
        this.marker = m;
        if (MarkerUtil.isFindBugsMarker(marker)) {
            bug = MarkerUtil.findBugInstanceForMarker(marker);
            file = (IFile) (marker.getResource() instanceof IFile ? marker.getResource() : null);
            javaElt = MarkerUtil.findJavaElementForMarker(marker);

            pattern = bug != null ? bug.getBugPattern() : null;
            refreshTitle();
            refreshAnnotations();
            refreshBrowser();
            rootComposite.layout(true, true);
        }
    }


}
