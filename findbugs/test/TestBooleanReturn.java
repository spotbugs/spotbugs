

public class TestBooleanReturn
{
	public boolean test(int a, int b)
	{
		if (a < b)
			return true;
		else
			return false;
	}
}

/* generates

  TestingGround: [0002]  if_icmpge
  TestingGround: [0005]  iconst_1
  TestingGround: [0006]  ireturn
  TestingGround: [0007]  iconst_0
  TestingGround: [0008]  ireturn
  
*/