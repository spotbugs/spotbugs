

class Super
{
	public void test1()
	{
	}

	public int test2(String s)
	{
		return 1;
	}

	public double test3(double d, int i, Object o, float f, long l)
	{
		return 0.0;
	}
}

public class UselessSCMethods extends Super
{
	public void test1()
	{
		super.test1();
	}

	public int test2(String s)
	{
		return super.test2(s);
	}

	public double test3(double d, int i, Object o, float f, long l)
	{
		return super.test3(d, i, o, f, l);
	}
}