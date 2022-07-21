package ghIssues;

import java.security.SecureRandom;
import java.util.Random;

public class Issue1464{
    // @ExpectWarning("DMI_RANDOM_USED_ONLY_ONCE")
    long m1(){
        return new SecureRandom().nextLong();
    }
    
    // @ExpectWarning("DMI_RANDOM_USED_ONLY_ONCE")
    long m2(){
        return new Random().nextLong();
    }
}
