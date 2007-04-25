

public class RandomStuff {

	/**
	 * @param args
	 */

		public void click()
		{

		}

		public void type( int x)
		{

		}

		public int typeRandom()
		{
			return 1;
		}

		public void screwAround()
		{
			int choice=(int)(Math.random()*5);
			switch (choice)
			{
				case 0:
					click();
					click();
//					choice=(int)Math.random()*5;
					break;
				case 1:
					type(typeRandom());
//					choice=(int)Math.random()*5;
					break;
				case 2:
//					choice=(int)Math.random()*5;
					System.out.println(typeRandom());
					break;
				case 3:
					click();
					break;
				case 4:

				default:
					choice=(int)Math.random()*5;
					break;
			}
		}
}
