package sfBugs;

public class Bug3460132 {
    String s = String.format("%2147483648$s", "");
}
