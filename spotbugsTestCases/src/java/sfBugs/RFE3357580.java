package sfBugs;

import javax.annotation.PreDestroy;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class RFE3357580 {
    
    @NoWarning("UPM_UNCALLED_PRIVATE_METHOD")
    @javax.annotation.PostConstruct
    private void postConstruct() { 
        System.out.println("getting ready to construct:");
    }
    
    @NoWarning("UPM_UNCALLED_PRIVATE_METHOD")
    @PreDestroy
    private void preDestroy() { 
        System.out.println("getting ready to destroy:");
    }
    
}
