

public class SBConcatTest
{
	public String doConcat1( String[] tokens )
	{
		//Detector should complain about the following
		String result = "";
		for (int i = 0; i < tokens.length; i++)
			result += tokens[i];
		return result;
	}

	public String doConcat2( String[] tokens )
	{
		//Detector should complain about the following
		String result = "";
		int i = 0; 
		while (i < tokens.length)
		{
			tokens[i] = tokens[i].trim();
			result = result + tokens[i];
			i++;
		}
		return result;			
	}
	
	public String doConcat3( String[] tokens )
	{
		// should not complain
		StringBuffer result = new StringBuffer();
		result.append("Results:" );
		for (int i = 0; i < tokens.length; i++)
			result.append( tokens[i] );
		return result.toString();
	}
	
	public String doConcat4( String[] tokens )
	{
		// should not complain
		int a = 0, b = 1, c = 2, d = 3, e = 4, f = 5, g = 6;
		StringBuffer result = new StringBuffer();
		result.append("Results:" );
		for (int i = 0; i < tokens.length; i++)
			result.append( tokens[i] );
		return result.toString();
	}

	public void doConcat5( String[] tokens ) 
	{
		// should not complain
		for(int i = 0; i < tokens.length; i++) 
			tokens[i] += i;
	}
}
