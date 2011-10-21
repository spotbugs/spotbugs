package edu.umd.cs.findbugs.bluej;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

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
        Log.setPath(new File(bluej.getSystemLibDir(), "findbugs.errlog"));
        bluej.setMenuGenerator(new RegularMenuBuilder(bluej));
        bluej.setPreferenceGenerator(new FindBugsPreferences(bluej));
        bluej.addPackageListener(new PackageClosingListener());
    }

    @Override
    public String getName()
    {
        return "FindBugs";
    }

    @Override
    public String getVersion()
    {
        return "1.3.5-dev";
    }

    @Override
    public String getDescription()
    {
        return "Extension for FindBugs, a static analysis tool that looks for coding errors.";
    }

    @Override
    public URL getURL()
    {
        try
        {
            return new URL("http://findbugs.sourceforge.net");
        }
        catch (MalformedURLException e) {return null;}
    }
}
