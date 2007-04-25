

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class UserMistakes {
	public static void main(String[] args) throws IOException {
		String name="Mr. Ed";
		name=name.replaceAll(".", "s.");
		System.out.println(name);


		//FIXME:FindBugs only catches this error with name.indexOf(String)
		if (name.indexOf("s") > 0)
			System.out.println("Yay");
		else
			System.out.println("Boo");

		String result;

		try 
		{
			BufferedReader findFiles=new BufferedReader(new FileReader("/mainList.txt"));
			if (findFiles.readLine()!=null)
				result=findFiles.readLine();
			findFiles.close();
		} catch (FileNotFoundException e) {
			System.exit(7);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}



		LineNumberReader tmp=new LineNumberReader(new FileReader("/mainList.txt"));
		int count = 0;
		while (tmp.readLine()!=null) count++;
          
		tmp.close();
}

}
