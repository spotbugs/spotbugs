package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * This class is the basic Quick Fix resolution for FindBugs. It uses a standard pattern to run the fixes.
 * Subclasses must use the ASTRewrite and the AST to make their changes. They are not responsible for the
 * setup and saving of the changes.
 * 
 * @author cchristopher@ebay.com
 */
public abstract class FindBugsFixer implements IMarkerResolution  {
	static private final String MISSING_BUG_INSTANCE = "This bug is no longer in the system. " +
	 "The bugs somehow got out of sync with the memory representation. " +
	 "Try running FindBugs again. If that does not work, check the error log and remove the .fbwarnings files.";

	 public void run(IMarker marker) {
		try {
			ICompilationUnit unit;
			String oldSource, newSource;
			Document doc;
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			CompilationUnit astRoot;
			ASTRewrite rewrite;
			TextEdit edits;
			IProject project = marker.getResource().getProject();
			
			unit = getCompilationUnit(marker);
			oldSource = unit.getBuffer().getContents();
			
			unit.becomeWorkingCopy(null, null);
			
			doc = new Document(oldSource);
			
			parser.setSource(unit);
			parser.setResolveBindings(resolveBindings());
			astRoot = (CompilationUnit)parser.createAST(null);
			
			rewrite = ASTRewrite.create(astRoot.getAST());
			
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			
			if (bug == null) {
				MessageDialog.openError(FindbugsPlugin.getShell(), "Missing Bug", MISSING_BUG_INSTANCE);
				return;
			}
			boolean changed = modify(rewrite, astRoot, bug);
			
			if (changed) {
				edits = rewrite.rewriteAST(doc, unit.getJavaProject().getOptions(true));
				edits.apply(doc);
				newSource = doc.get();
				
				unit.getBuffer().setContents(newSource);
				unit.commitWorkingCopy(false, null);
				
				SortedBugCollection bugCollection = FindbugsPlugin.getBugCollection(project, null);
				bugCollection.remove(bug);
				
				marker.delete();
				MarkerUtil.redisplayMarkers(marker.getResource().getProject(), FindbugsPlugin.getShell());
			}
			unit.discardWorkingCopy();
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	protected abstract boolean resolveBindings();

	protected abstract boolean modify(ASTRewrite rewrite, CompilationUnit astRoot, BugInstance bug);

	/**
	 * Get the compilation unit for the marker
	 * @param marker
	 * @return The compilation unit for the marker, or null if the file was not accessible
	 * or was not a Java file.
	 */
	@CheckForNull
	private ICompilationUnit getCompilationUnit(IMarker marker) {
		IResource res = marker.getResource();
		if (res instanceof IFile && res.isAccessible()) {
			IJavaElement element = JavaCore.create((IFile)res);
			if (element instanceof ICompilationUnit)
				return (ICompilationUnit)element;
		}
		return null;
	}
	
	/**
	 * Search for a TypeDeclaration with a specific name.
	 * @param astRoot The compilation unit to search in.
	 * @param className The qualified class name to search for.
	 * @return The ASTNode in astRoot that matches className, or null if nothing was found
	 */
	@CheckForNull
	protected TypeDeclaration getTypeDeclaration(CompilationUnit astRoot, String className) {
		Iterator itr;
		ASTNode node = null;
		TypeDeclaration ret = null;
		String packageName = "", baseName, lookingForPackage;
		String[] innerClasses;
		
		baseName = className.substring(className.lastIndexOf(".") + 1, className.length());
		lookingForPackage = className.substring(0, className.lastIndexOf("."));
		innerClasses = baseName.split("\\$");

		if (astRoot.getPackage() != null)
			packageName = astRoot.getPackage().getName().getFullyQualifiedName();
		
		if (!packageName.equals(lookingForPackage)) {
			return null;
		}
		
		itr = astRoot.types().iterator();

		while (ret == null && itr.hasNext()) {
			node = (ASTNode)itr.next();
			
			if (node instanceof TypeDeclaration) {
				ret = getTypeDeclaration((TypeDeclaration) node, innerClasses, 0);
				
			}
		}
		
		return ret;
	}
	
	private TypeDeclaration getTypeDeclaration(TypeDeclaration node, String[] innerClasses, int depth) {
		String simple = node.getName().getIdentifier();
		
		if (!simple.equals(innerClasses[depth]))
			return null;
		
		if (depth == innerClasses.length - 1)
			return node;
		
		TypeDeclaration[] types = node.getTypes();
		
		for (int ndx = 0; ndx < types.length; ndx++) {
			TypeDeclaration foundChild = getTypeDeclaration(types[ndx], innerClasses, depth + 1);
			
			if (foundChild != null)
				return foundChild;
		}
		return null;
	}
	
	/**
	 * Search for a FieldDeclaration with a specific name.
	 * @param type The TypeDeclaration to search in
	 * @param fieldName The simple field name to search for.
	 * @return The ASTNode in type that matches fieldName, or null if nothing was found
	 */
	@CheckForNull
	protected FieldDeclaration getFieldDeclaration(TypeDeclaration type, String fieldName) {
		FieldDeclaration[] fields = type.getFields();
		FieldDeclaration ret = null;
		boolean found = false;
		
		for (int ndx = 0; ndx < fields.length && !found; ndx++) {
			ret = fields[ndx];
			List fragments = ret.fragments();
			
			if (fragments.size() == 1) {
				VariableDeclarationFragment var = (VariableDeclarationFragment)fragments.get(0);
				
				found = fieldName.equals(var.getName().getIdentifier());
			}
		}
		
		return found ? ret : null;
	}
	
	@CheckForNull
	protected MethodDeclaration getMethodDeclaration(TypeDeclaration type, String methodNameSig) {
		MethodDeclaration[] methods = type.getMethods();
		MethodDeclaration ret = null;
		boolean found = false;
		String methodName, params;
		String[] paramTypes;
		
		methodName = methodNameSig.substring(0, methodNameSig.indexOf('('));
		params = methodNameSig.substring(methodNameSig.indexOf('(') + 1, methodNameSig.length() - 1);
		
		paramTypes = params.split(",");
		
		for (int ndx = 0; ndx < paramTypes.length; ndx++)
			paramTypes[ndx] = paramTypes[ndx].trim();
		
		for (int ndx = 0; ndx < methods.length && !found; ndx++) {
			if (methods[ndx].getName().getIdentifier().equals(methodName)) {
				ret = methods[ndx];
				Iterator typeItr = ret.parameters().iterator();
				int paramNum = 0;
				found = true;
				while (typeItr.hasNext()) {
					Type paramType = ((SingleVariableDeclaration)typeItr.next()).getType();	
					String name = getPrettyTypeName(paramType);
					
					found = found && name.equals(paramTypes[paramNum]);
					paramNum++;
				}
			}
		}
		
		return found ? ret : null;
	}
	
	private String getPrettyTypeName(Type type) {
		if (type.isArrayType()) {
			return getPrettyTypeName((ArrayType)type);
		}
		else if (type.isParameterizedType()) {
			return getPrettyTypeName((ParameterizedType)type);
		}
		else if (type.isPrimitiveType()) {
			return getPrettyTypeName((PrimitiveType)type);
		}
		else if (type.isQualifiedType()) {
			return getPrettyTypeName((QualifiedType)type);
		}
		else if (type.isSimpleType()) {
			return getPrettyTypeName((SimpleType)type);
		}
		else {
			return "";
		}
	}
	
	private String getPrettyTypeName(ArrayType type) {
		return getPrettyTypeName(type.getComponentType()) + "[]";
	}
	
	private String getPrettyTypeName(PrimitiveType type) {
		return type.getPrimitiveTypeCode().toString();
	}
	
	private String getPrettyTypeName(ParameterizedType type) {
		String ret = type.getType() + "<";
		Iterator itr = type.typeArguments().iterator();
		
		while (itr.hasNext()) {
			Type arg = (Type)itr.next();
			ret = ret + getPrettyTypeName(arg);
			
			if (itr.hasNext())
				ret = ret + ",";
		}
		ret = ret + ">";
		
		return ret;
	}

	private String getPrettyTypeName(QualifiedType type) {
		return type.resolveBinding().getQualifiedName();
	}

	private String getPrettyTypeName(SimpleType type) {
		return type.resolveBinding().getQualifiedName();
	}
}
