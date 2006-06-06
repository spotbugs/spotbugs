// This line added to make sure I can do a CVS commit.

package edu.umd.cs.findbugs.bluej.test;
import bluej.extensions.BlueJ;
import bluej.extensions.Extension;

/**
 * A uselessly simple extension for BlueJ which greets the world happily and
 * then lists all classes defined in all packages in all open projects.
 * 
 * @author Reuven Lazarus
 * @version 1 Jun 2006
 */
public class HelloWorldExtension extends Extension
{
	/* Not strictly necessary, but Eclipse's narrow-minded JAR wizard won't make
	 * this the main class of the JAR unless it has a main method. For a real
	 * extension we can be less lazy and write the JAR manifest by hand, so we
	 * won't need this.
	 */
	public static void main(String[] args)
	{
		System.out.println("JAR is not executable.  Run the extension by"
				+ "placing this JAR in /lib/extensions from the BlueJ install"
				+ "directory and starting BlueJ.");
	}

	/**
	 * Returns whether the extension is compatible with the current version of
	 * BlueJ.
	 * 
	 * @return true, since we're not doing anything fancy
	 */
	@Override
	public boolean isCompatible()
	{
		return true;
	}

	/**
	 * Runs when the extension is started. Registers menu entry.
	 */
	@Override
	public void startup(BlueJ bluej)
	{
		bluej.setMenuGenerator(new MenuBuilder(bluej));
		bluej.addCompileListener(new ClassLister(bluej));
	}

	/**
	 * Get the user-visible extension name.
	 */
	@Override
	public String getName()
	{
		return "Hello World Extension";
	}

	/**
	 * Get the extension version number.
	 */
	@Override
	public String getVersion()
	{
		return "0.1";
	}

}
