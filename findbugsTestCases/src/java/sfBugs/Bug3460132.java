package sfBugs;

public class Bug3460132 {
    
    String index = String.format("%2147483648$g", 42.0);
    String width = String.format("%2147483648g", 42.0);
    String precision = String.format("%.2147483648g", 42.0);
}
