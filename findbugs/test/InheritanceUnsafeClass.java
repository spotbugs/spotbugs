import java.net.URL;

public class InheritanceUnsafeClass
{
    public URL getResource(String r)
    {
        return getClass().getResource(r);
    }
}

