package nullnessAnnotations.packageDefault_JDT;

interface Interface1 {
    Object f(Object o); // this should inherit a NonNull definition from the
                        // package

    Object g(Object o); // this should inherit a NonNull definition from the
                        // package
}
