package sfBugs;

public class Bug3458406 {

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_STRING_CTOR", justification = "A unique copy of the String is needed.")
    private static final Object s_empty = new String("EMPTY"); // Guarantee a unique copy

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_STRING_CTOR", justification = "A unique copy of the String is needed.")
    private static final Object s_read = new String("READ"); // Guarantee a unique copy
    
    private static final Object s_empty2 = makeUniqueStringObject("EMPTY");
    private static final Object s_read2 = makeUniqueStringObject("EMPTY"); 
  
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_STRING_CTOR", justification = "A unique copy of the String is needed.")
    private static String makeUniqueStringObject(String s) {
        return new String(s);
    }
   

    
}
