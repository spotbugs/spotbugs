import java.net.URL;
import java.io.InputStream;

public class InheritanceUnsafeClass
{
    public URL getResource(String r)
    {
        return getClass().getResource(r);
    }
}

class InheritanceUnsafeClass2
{	
	public InputStream getResourceBAD(String r)
	{
		return getClass().getResourceAsStream(r);
	}
}

class InheritanceUnsafeClass3
{
	public InputStream getResourceOK(String r)
	{//This should not report a bug
		return InheritanceUnsafeClass.class.getResourceAsStream(r);
	}
}

