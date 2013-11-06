package sfBugs;




public class NBug1167
{
//    @ExpectWarning("NP_ALWAYS_NULL")
//    public void fails() 
//    {
//        ResultSet rs = null;
//
//        try
//        {
//            Class dbiClass = Class.forName( "" );
//        }
//        catch( Exception e )
//        {
//        }
//
//        while( true  )
//        {
//            try
//            {
//                if( rs.next() )
//                {
//                    String retVal = "";
//                }
//            }
//            catch( Exception e )
//            {
//                StackTracePrinter.toString(e);
//            }
//            finally
//            {
//                try
//                {
//                    rs.close();
//                }
//                catch( Throwable t )
//                {
//                    // ignore
//                }
//            }
//        }
//    }

	private static class StackTracePrinter
	{
		public static String toString(Exception e) {
			return "";
		}
	}
}

