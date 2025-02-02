package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

class MultipleInstantiationsOfSingletonsTest extends AbstractIntegrationTest {
    @Test
    void abstractClassTest() {
        performAnalysis("singletons/AbstractClass.class",
                "singletons/AbstractClass$Nested.class");
        assertNoBugs();
    }

    @Test
    void innerChildInstanceTest() {
        performAnalysis("singletons/InnerChildInstance.class",
                "singletons/InnerChildInstance$Unknown.class");
        assertNoBugs();
    }

    @Test
    void innerChildAndMoreInstanceTest() {
        performAnalysis("singletons/InnerChildAndMoreInstance.class",
                "singletons/InnerChildAndMoreInstance$Unknown.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "InnerChildAndMoreInstance");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void InnerChildAndMoreInstanceReorderedTest() {
        performAnalysis("singletons/InnerChildAndMoreInstanceReordered.class",
                "singletons/InnerChildAndMoreInstanceReordered$Unknown.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "InnerChildAndMoreInstanceReordered");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void InnerChildAndMoreInstanceNoGetterTest() {
        performAnalysis("singletons/InnerChildAndMoreInstanceNoGetter.class",
                "singletons/InnerChildAndMoreInstanceNoGetter$Unknown.class");
        assertNoBugs();
    }

    @Test
    void notSingletonWithFactoryMethodTest() {
        performAnalysis("singletons/NotSingletonWithFactoryMethod.class");
        assertNoBugs();
    }

    @Test
    void notLazyInitSingletonTest() {
        performAnalysis("singletons/NotLazyInitSingleton.class");
        assertNoBugs();
    }

    @Test
    void cloneableSingletonTest() {
        performAnalysis("singletons/CloneableSingleton.class");
        assertBugTypeCount("SING_SINGLETON_IMPLEMENTS_CLONEABLE", 1);
        assertBugInMethod("SING_SINGLETON_IMPLEMENTS_CLONEABLE", "CloneableSingleton", "clone");

        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void indirectlyCloneableSingletonTest() {
        performAnalysis("singletons/IndirectlyCloneableSingleton.class", "singletons/CloneableClass.class");
        assertBugTypeCount("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE", 1);
        assertBugInClass("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE", "IndirectlyCloneableSingleton");

        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void defaultConstructorTest() {
        performAnalysis("singletons/DefaultConstructor.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "DefaultConstructor");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void protectedConstructorTest() {
        performAnalysis("singletons/ProtectedConstructor.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "ProtectedConstructor");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void protectedConstructorStaticInitTest() {
        performAnalysis("singletons/ProtectedConstructorStaticInit.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "ProtectedConstructorStaticInit");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void protectedConstructorStaticInitReorderedTest() {
        performAnalysis("singletons/ProtectedConstructorStaticInitReordered.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "ProtectedConstructorStaticInitReordered");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void notCloneableButHasCloneMethodTest() {
        performAnalysis("singletons/NotCloneableButHasCloneMethod.class");
        assertBugTypeCount("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", 1);
        assertBugInMethod("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", "NotCloneableButHasCloneMethod", "clone");

        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void serializableSingletonTest() {
        performAnalysis("singletons/SerializableSingleton.class");
        assertBugTypeCount("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE", 1);
        assertBugInClass("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE", "SerializableSingleton");

        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void notSynchronizedSingletonTest() {
        performAnalysis("singletons/NotSynchronizedSingleton.class");
        assertBugTypeCount("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", 1);
        assertBugInMethod("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "NotSynchronizedSingleton", "getInstance");

        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
    }

    @Test
    void inappropriateSynchronization() {
        performAnalysis("singletons/InappropriateSynchronization.class");
        // now cannot detect synchronization bug
        // because of the using of a monitor inside function
        assertBugTypeCount("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", 0);
        //assertSINGBug("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "InappropriateSynchronization", "getInstance");

        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
    }

    @Test
    void appropriateSingletonTest() {
        performAnalysis("singletons/AppropriateSingleton.class");
        assertNoBugs();
    }

    @Test
    void initializeOnDemandHolderIdiomTest() {
        performAnalysis("singletons/InitializeOnDemandHolderIdiom.class",
                "singletons/InitializeOnDemandHolderIdiom$SingletonHolder.class");
        assertNoBugs();
    }

    @Test
    void enumSingletonTest() {
        performAnalysis("singletons/EnumSingleton.class");
        assertNoBugs();
    }

    @Test
    void oneEnumSingletonTest() {
        performAnalysis("singletons/OneEnumSingleton.class");
        assertNoBugs();
    }

    @Test
    void unconditionalThrowerCloneableTest() {
        performAnalysis("singletons/UnconditionalThrowerCloneable.class");
        assertNoBugs();
    }

    @Test
    void doubleCheckedLockingTest() {
        performAnalysis("singletons/DoubleCheckedLocking.class");
        assertNoBugs();
    }

    @Test
    void doubleCheckedLockingIndirectTest() {
        performAnalysis("singletons/DoubleCheckedLockingIndirect.class");
        assertNoBugs();
    }

    @Test
    void doubleCheckedLockingIndirect2Test() {
        performAnalysis("singletons/DoubleCheckedLockingIndirect2.class");
        assertNoBugs();
    }

    @Test
    void multiplePrivateConstructorsTest() {
        performAnalysis("singletons/MultiplePrivateConstructors.class");
        assertNoBugs();
    }

    @Test
    void multipleNonPrivateConstructorsTest() {
        performAnalysis("singletons/MultipleNonPrivateConstructors.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "MultipleNonPrivateConstructors");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    void multipleNonPrivateConstructorsTest2() {
        performAnalysis("singletons/MultipleNonPrivateConstructors2.class");
        assertBugTypeCount("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertBugInClass("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "MultipleNonPrivateConstructors2");

        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void recordNotSingletonTest() {
        performAnalysis("../java17/Issue2981.class");
        assertNoBugs();
    }

    private void assertNoBugs() {
        assertNoBugType("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD");
        assertNoBugType("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE");
        assertNoBugType("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED");
    }
}
