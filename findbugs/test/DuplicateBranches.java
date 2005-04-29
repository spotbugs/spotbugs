
public class DuplicateBranches
{
	public static int doit(boolean b, int i, int j)
	{
		int z;
		if (b)
		{
			int k = i * j;
			z = k / 100;
		}
		else
		{
			int k = i * j;
			z = k / 100;
		}
		return z;
	}
}
