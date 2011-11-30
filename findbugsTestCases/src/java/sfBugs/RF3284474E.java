package sfBugs;

import java.util.List;

public class RF3284474E {

   /**
    * I encountered the following code within a project and I think 
    * FindBugs should provide at least a warning:
    *  for (Row row : rowList.getRows()) {
    *   return row.findSomething(row);
    *   }
    */

    public int getFoo(List<String> lst) {
        for(String s : lst)
            return s.length();
        return 0;
    }
}
