package sfBugs;

public class Bug1524063 {
    
    void fizzBuzz() {
        String x;
        
        // this method should not generate a string concatenation in loop warning
        for(int i = 0; i < 100; i++) {
            x = "";
            if (i % 3 == 0) x = "Fizz";
            if (i % 5 == 0) x += "Buzz";
            if (x == "") x += i;
            System.out.println(i);
        }
    }

}
