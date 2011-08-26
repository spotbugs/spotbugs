
package de.tobject.findbugs.view;

import java.net.MalformedURLException;
import java.net.URL;

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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.graphics.Color;
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
import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
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
import edu.umd.cs.findbugs.util.ClassName;

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

   private String title;

   private IJavaElement javaElt;

   private FormToolkit toolkit;

   private ISelectionListener selectionListener;



   /**
    *
    */
   public BugInfoView() {

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
       if (false) {
        rootComposite.setBackground(background);
    }
       }

   private void createBrowser(Composite parent) {
       Color background = toolkit.getColors().getBackground();
       GridData data = new GridData(GridData.FILL_BOTH);
       try {
           browser = new Browser(parent, SWT.NO_BACKGROUND);
           browser.setLayoutData(data);

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
                     FindbugsPlugin plugin = FindbugsPlugin.getDefault();
               plugin.logException(new RuntimeException(e.getMessage(), e),
                       "Could not create org.eclipse.swt.widgets.Composite.Browser");

       }
   }

   private  void makeAnnotationList(Composite panel) {
       annotationList = new List(panel, SWT.NONE);
       GridData data = new GridData(GridData.FILL_HORIZONTAL);
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
       annotationList.setToolTipText("Click on lines or methods to go to them");
       annotationList.setMenu(menu);
       annotationList.pack(true);

   }

   private void updateDisplay() {
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
       text.append(pattern.getDetailText());

       BugCategory category = DetectorFactoryCollection.instance().getBugCategory(pattern.getCategory());
       text.append("<p> Category: " + category.getShortDescription());
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

   /**
    * Updates pattern and bug from selection
    *
    * @return true if the content is changed
    */
   private boolean contentChanged(ISelection selection) {
       if (showingAnnotation) {
        return false;
    }
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
               rootComposite.layout();
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
           for(BugAnnotation ba : bug.getAnnotationsForMessage(false)) {
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
            IProject p =  file.getProject();
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

    private volatile boolean showingAnnotation = false;

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
            int line = marker.getAttribute(IMarker.LINE_NUMBER, EditorUtil.DEFAULT_LINE_IN_EDITOR);
            EditorUtil.goToLine(activeEditor, line);
        } finally {
            showingAnnotation = false;
        }
    }

    private String stripFirstAndLast(String s) {
        return s.substring(1, s.length()-1);
    }
    private IMethod getIMethod(IType type, MethodAnnotation mma) throws JavaModelException {
        String name = mma.getMethodName();
        SignatureParser parser = new SignatureParser(mma.getMethodSignature());
        String[] arguments = parser.getArguments();


        nextMethod: for(IMethod m : type.getMethods()) {
            if (!m.getElementName().equals(name)) {
                continue nextMethod;
            }

                String [] mArguments = m.getParameterTypes();
                if (arguments.length != mArguments.length) {
                    continue nextMethod;
                }

                for(int i = 0; i < arguments.length; i++) {
                    String a = arguments[i];
                    String ma = mArguments[i];
                    while (a.startsWith("[") && ma.startsWith("[")) {
                        a = a.substring(1);
                        ma = ma.substring(1);
                    }
                    if (ma.startsWith("Q")) {
                        ma = stripFirstAndLast(ma);
                        ClassDescriptor ad = ClassDescriptor.fromFieldSignature(a);
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
        if (showingAnnotation) {
            return;
        }
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
            rootComposite.layout();
        }
    }


}
