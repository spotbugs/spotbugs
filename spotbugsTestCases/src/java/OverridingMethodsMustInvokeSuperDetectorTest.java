import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class OverridingMethodsMustInvokeSuperDetectorTest {
    public static class GenericClass<X > {
        @edu.umd.cs.findbugs.annotations.OverridingMethodsMustInvokeSuper
        public void genericMethod1( X obj ) {
        }

        @javax.annotation.OverridingMethodsMustInvokeSuper
        public void genericMethod2( X obj ) {
        }
    }

    public class ConcreteClass extends GenericClass<String > {
    }

    public class DerivedClass extends ConcreteClass {
        @ExpectWarning( "OVERRIDING_METHODS_MUST_INVOKE_SUPER" )
        @Override
        public void genericMethod1( String obj ) {
            // no call to super.genericMethod( obj )
        }

        @ExpectWarning( "OVERRIDING_METHODS_MUST_INVOKE_SUPER" )
        @Override
        public void genericMethod2( String obj ) {
            // no call to super.genericMethod( obj )
        }
    }
}
