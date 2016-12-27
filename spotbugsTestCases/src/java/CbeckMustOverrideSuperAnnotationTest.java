import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.OverridingMethodsMustInvokeSuper;

public class CbeckMustOverrideSuperAnnotationTest {
    public static class GenericClass<T > {
        @OverridingMethodsMustInvokeSuper
        public void genericMethod( T obj ) {
        }
    }

    public class ConcreteClass extends GenericClass<String > {
    }

    public class CLtbe_AgentCallback extends ConcreteClass {
        @ExpectWarning( "OVERRIDING_METHODS_MUST_INVOKE_SUPER" )
        @Override
        public void genericMethod( String obj ) {
            // no call to super.genericMethod( obj )
        }
    }
}
