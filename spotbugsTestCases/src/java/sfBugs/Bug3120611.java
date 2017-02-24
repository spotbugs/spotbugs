package sfBugs;

public class Bug3120611 {

        private Bug3120611() { }

        /** Intended as a _static initializer block; never executed */
        { System.out.println("Hello world"); }


}
