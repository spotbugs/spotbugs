package edu.umd.cs.findbugs.gui2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;


/**
 * These are the leaves of the tree, note that coloring is not done here, it is done in BugRenderer
 * This class is basically just a wrapper for BugInstance
 */

// only thing of note is the equals method, which purposefully uses == since JTree's only show one of multiple equal objects.

public class BugLeafNode {
	
	private BugInstance bug;
	
	BugLeafNode(BugInstance b)
	{
		bug=b;
	}
	
	public BugInstance getBug()
	{
		return bug;
	}
	
	public String toString()
	{
		
		
		return bug.getMessageWithoutPrefix();
	}

	public boolean matches(StringPair keyValuePair) {
/*		
		try
		{
			Method m = BugInstance.class.getMethod("get" + keyValuePair.key,new Class[0]);
			return (keyValuePair.value.equals(m.invoke(this,new Object[0])));
		}
		catch(SecurityException e)
		{
			System.err.println("NoOoOOOooOoOo000!!!1!!!1one!");
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("get" + keyValuePair.key + " does not exist");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("Make the method get" + keyValuePair.key + " public or package or ... something.  ..  Now.");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
*/	
		return keyValuePair.key.getFrom(bug).equals(keyValuePair.value);
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof BugLeafNode))
			return false;
		else
			return bug==(((BugLeafNode)o).getBug());
	}

	
	public int hashCode()
	{
		if (bug.getUniqueId() != null)
			return bug.getUniqueId().hashCode() + 7;
		else
			return bug.hashCode() + 7;
	}
	
	public boolean matches(BugAspects aspects) {
		if(aspects.size() == 0)
			return true;
		for(BugAspects.StringPair strPair : aspects){
			if(!matches(strPair))
				return false;
		}
		
		return true;
	}
}


