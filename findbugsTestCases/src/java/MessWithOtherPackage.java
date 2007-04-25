
import messWithMe.DumbPublicClass;
import messWithMe.UnprotectedClass;
public class MessWithOtherPackage extends DumbPublicClass //Hoohoo!
{	
	public static void main(String[] args) {
		UnprotectedClass.DontMessWithMe.setX(7);//Whuahahahhaa

		//		MUAUAHAHAHAHAHAHHAHA
		System.out.println(MessWithOtherPackage.CreditCardNumbers[0]);
		System.out.println(MessWithOtherPackage.CreditCardNumbers[1]);
		System.out.println(MessWithOtherPackage.CreditCardNumbers[2]);
		System.out.println(MessWithOtherPackage.CreditCardNumbers[3]);
		System.out.println(MessWithOtherPackage.CreditCardNumbers[4]);

		//TODO?  If Only Findbugs Could Catch This...
	/* Although the CreditCardNumbers array is package, 
	 * and the interface it is in is also package, 
	 * there was a public class that implemented that 
	 * interface and gave access to this evil program*/


	}
}
