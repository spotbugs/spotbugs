package ghIssues;

public class Issue1472 {
    int testSA_LOCAL_SELF_COMPUTATION(){
        int flags = 1;
        return flags ^ flags; // expeected SA_LOCAL_SELF_COMPUTATION
    }

    int testSA_LOCAL_SELF_COMPUTATION1(){
        int flags = 1;
        return flags & flags; //  expeected SA_LOCAL_SELF_COMPUTATION
    }

    int testSA_LOCAL_SELF_COMPUTATION2(){
        int flags = 1;
        return flags - flags; //  expeected SA_LOCAL_SELF_COMPUTATION
    }

    int testSA_LOCAL_SELF_COMPUTATION3(){
        int flags = 1;
        return flags | flags; //  expeected SA_LOCAL_SELF_COMPUTATION
    }
}
