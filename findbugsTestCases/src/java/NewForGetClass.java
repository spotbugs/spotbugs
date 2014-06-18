import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class NewForGetClass {
    @ExpectWarning("ISC_INSTANTIATE_STATIC_CLASS")
    public static void main(String[] args) {
        // Just do : Class c = NewForGetClass.class;
        Class c = new NewForGetClass().getClass();
    }
}
