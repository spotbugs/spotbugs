package edu.umd.cs.findbugs.bluej;

import bluej.extensions.BlueJ;
import bluej.extensions.Extension;

public class FindBugsExtension extends Extension
{

	@Override
	public boolean isCompatible()
	{
		return true;
	}

	@Override
	public void startup(BlueJ bluej)
	{
		bluej.setMenuGenerator(new RegularMenuBuilder());
		bluej.addCompileListener(new RunFindbugs(bluej));
	}

	@Override
	public String getName()
	{
		return "FindBugs";
	}

	@Override
	public String getVersion()
	{
		return "0.1";
	}

}
